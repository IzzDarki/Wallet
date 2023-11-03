package com.izzdarki.wallet.services

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.os.Parcelable
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.Field
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.InlinePresentation
import android.service.autofill.Presentations
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.util.Log
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import kotlinx.parcelize.Parcelize
import com.izzdarki.wallet.data.Credential
import com.izzdarki.wallet.data.CredentialField
import com.izzdarki.wallet.logic.autofill.AutofillLogicalGroup
import com.izzdarki.wallet.logic.autofill.findDataSourcesForRequest
import com.izzdarki.wallet.logic.autofill.groupOf
import com.izzdarki.wallet.logic.autofill.valueGivenAutofillHints
import com.izzdarki.wallet.logic.autofill.valueGivenHintAndText
import com.izzdarki.wallet.logic.isAuthenticationEnabled
import com.izzdarki.wallet.storage.CredentialPreferenceStorage
import com.izzdarki.wallet.ui.authentication.AutofillAuthenticationActivity
import izzdarki.wallet.R

/**
 * Captures all information about a view that is eligible for autofill.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Parcelize
data class AutofillViewData(
    val autofillId: AutofillId,
    val autofillHints: List<String>,
    val hint: String?,
    val text: String?,
    val isFocused: Boolean = false,
) : Parcelable

@RequiresApi(Build.VERSION_CODES.O)
class WalletAutofillService : AutofillService() {

    override fun onFillRequest(fillRequest: FillRequest, cancellationSignal: CancellationSignal, fillCallback: FillCallback) {

        // Find out where the user is trying to fill
        val fillContexts = fillRequest.fillContexts
        val latestStructure = fillContexts[fillContexts.size - 1].structure
        val webDomain = getWebDomain(latestStructure)
        val packageName =
            getPackageName(latestStructure)  // If webDomain is not null, this is considered the package name of the browser => use webDomain
        Log.d("autofill", "---\nFill request for web domain $webDomain and package name $packageName")

        // Find the data sources that match the request (more than one if the user has multiple accounts for the same website)
        val suitableDataSources = findDataSourcesForRequest(
            allCredentials = CredentialPreferenceStorage.readAllCredentials(this),
            webDomain,
            packageName
        )

        // Extract all necessary information from the structure
        val autofillViewsData = traverseStructureToGetAutofillViewData(latestStructure)

        // Find all autofill ids of views that can be filled using the given data sources
        val fillableAutofillIds = suitableDataSources.flatMap { dataSource ->
            extractValuesToFillViews(dataSource, autofillViewsData).map { it.first }
            // This already determines what to fill for each view, but then only remembers the autofill id
            // The actual filling is done (again) after the user authenticated
            // It might be not secure to send the actual values through the PendingIntent that is sent to the authentication activity
        }

        if (fillableAutofillIds.isEmpty()) { // No views can be filled
            fillCallback.onSuccess(null) // null means nothing can be filled
            return
        }


        val fillResponseBuilder = FillResponse.Builder()
        if (isAuthenticationEnabled(this)) {
            // Create fill response without any data, just for the authentication
            // Still, since an unauthenticated user can see what views can be auto-filled, this leaks some information about what data is stored to unauthenticated users
            addAuthenticationToFillResponse(
                fillResponseBuilder,
                suitableDataSources,
                autofillViewsData,
                fillableAutofillIds.toTypedArray()
            )
        } else {
            addDatasetsToFillResponse(fillResponseBuilder, suitableDataSources, autofillViewsData)
            // Will always add at least one dataset, since fillableAutofillIds is not empty
        }

        fillCallback.onSuccess(fillResponseBuilder.build())
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        // Save request are not supported by this service
        callback.onFailure("Save requests are not supported by ${getString(R.string.wallet_autofill_service)}")
    }

    companion object {

        private const val AUTHENTICATION_REQUEST_CODE = 417023

        // Method to be called by AutofillAuthenticationActivity after the user authenticated
        fun Context.createFillResponseWithDatasets(dataSources: List<Credential>, autofillViewsData: List<AutofillViewData>): FillResponse? {
            val fillResponseBuilder = FillResponse.Builder()
            val couldAddAnyDatasets = addDatasetsToFillResponse(fillResponseBuilder, dataSources, autofillViewsData)
            if (!couldAddAnyDatasets)
                return null // null means nothing can be filled
            return fillResponseBuilder.build()
        }

        /**
         * @return `true` if at least one dataset could be added to the fill response, `false` otherwise
         */
        private fun Context.addDatasetsToFillResponse(
            fillResponseBuilder: FillResponse.Builder,
            dataSources: List<Credential>,
            autofillViewsData: List<AutofillViewData>
        ): Boolean {
            val datasets = dataSources.mapNotNull { dataSource ->
                val valuesForAutofill = extractValuesToFillViews(dataSource, autofillViewsData)
                createDataset(dataSource, valuesForAutofill) // null if for all views no value could be found using this data source
            }

            if (datasets.isEmpty()) // No views can be filled
                return false // nothing can be filled

            for (dataset in datasets)
                fillResponseBuilder.addDataset(dataset)
            return true
        }

        /**
         * Creates a [Dataset] containing all the given `autofillValues`
         * @param dataSource Data source that was used to find the values
         * @param autofillValues Mapping of [AutofillId]s to the [CredentialField]s, that should be used to fill them
         */
        private fun Context.createDataset(
            dataSource: Credential,
            autofillValues: List<Pair<AutofillId, CredentialField>>
        ): Dataset? {
            if (autofillValues.isEmpty())
                return null // nothing to fill

            // It is possible to add authentication to this dataset individually (user has to authenticate after clicking on the dataset)
            // Dataset.Builder().setAuthentication(...)
            // At the moment the user has to authenticate before seeing any datasets (fill request has authentication), which is sufficient

            val datasetBuilder = Dataset.Builder()
            for ((autofillId, credentialField) in autofillValues) {
                if (Build.VERSION.SDK_INT >= 33) {
                    // Create presentations
                    val presentationsBuilder = Presentations.Builder()
                    createInlinePresentation(
                        dataSource,
                        credentialField
                    )?.let { presentationsBuilder.setInlinePresentation(it) }
                    createInlineTooltipPresentation(
                        dataSource,
                        credentialField
                    )?.let { presentationsBuilder.setInlineTooltipPresentation(it) }
                    createDialogPresentation(
                        dataSource,
                        credentialField
                    )?.let { presentationsBuilder.setDialogPresentation(it) }
                    createMenuPresentation(
                        dataSource,
                        credentialField
                    )?.let { presentationsBuilder.setMenuPresentation(it) }

                    // Create field
                    val field =
                        Field.Builder() // Only available in API 33+ // TODO Test this on API 33+ device
                            .setValue(AutofillValue.forText(credentialField.value))
                            .setPresentations(presentationsBuilder.build())
                            .build()

                    // Add field to dataset
                    datasetBuilder.setField(autofillId, field)
                } else {
                    @Suppress("DEPRECATION")
                    datasetBuilder.setValue(
                        autofillId,
                        AutofillValue.forText(credentialField.value),
                        createRemoteViewsPresentation(dataSource, credentialField)
                    )
                }
            }
            return datasetBuilder.build()
        }

        /**
         * Tries to find values to be filled for the given [AutofillViewData]s using the given [Credential].
         * Finds out what logical group the request belongs to and only returns values for views that belong to that group.
         *
         * Using logical groups is very strongly recommended by the autofill framework and has the following benefits
         *  - It's easier to predict what will be filled
         *  - It's more secure, since it's harder to trick to user into filling a value (for ex. in an invisible input field) (is that an actual problem?)
         *
         * @return A list of [AutofillId]s and the corresponding [CredentialField]s.
         *  Only includes [AutofillId]s of views that can be filled.
         */
        private fun extractValuesToFillViews(
            dataSource: Credential,
            viewsData: List<AutofillViewData>,
        ): List<Pair<AutofillId, CredentialField>> {
            val focusedViewDataIndex = viewsData.indexOfFirst { it.isFocused }.let { if (it == -1) null else it }
            val zipped = viewsData.map {
                val autofillValue = autofillValueForNode(dataSource, it)
                val logicalGroup = if (autofillValue == null) null else groupOf(it, autofillValue.second)
                Triple(it, autofillValue, logicalGroup)
            }

            // Determine what logical group this request belongs to
            val logicalGroup = if (focusedViewDataIndex != null) {
                // If a view is focused => use the group of the focused view
                val focusedAutofillValue = zipped[focusedViewDataIndex].second
                    ?: return emptyList() // If focused view cannot be filled, the fill request is not answered
                groupOf(viewsData[focusedViewDataIndex], focusedAutofillValue.second)
            } else {
                // If no view is focused => use the first group that could be found
                zipped.firstNotNullOfOrNull { (_, _, group) -> group}
                    ?: AutofillLogicalGroup.OTHER // no value found for any view => assume "other"
            }

            // Only return values for views that belong to the determined logical group
            return zipped.mapNotNull { (_, autofillValue, group) ->
                if (autofillValue == null || group == logicalGroup)
                    null // exclude if no value could be found or group does not match
                else
                    autofillValue // include otherwise
            }

        }

        /**
         * For a view with different logical groups according to autofillHints and hint/text,
         * it is unclear what group is actually used => The group used to fill will be considered,
         * if that is unclear (<=> field name and type are unknown) fallback to autofillHints, then hint/text then assume no group.
         */

        /**
         * Traverses the structure to find all [AutofillViewData]s.
         * Goes through all windows nodes (what is a window node?).
         */
        private fun traverseStructureToGetAutofillViewData(structure: AssistStructure): List<AutofillViewData> {
            return (0 until structure.windowNodeCount).flatMap { windowIndex ->
                traverseStructureFromRootNodeToGetAutofillViewData(
                    structure.getWindowNodeAt(
                        windowIndex
                    ).rootViewNode
                )
            }
        }

        /**
         * Traverses the structure to find all [AutofillViewData]s.
         * Starts at a given root node.
         */
        private fun traverseStructureFromRootNodeToGetAutofillViewData(currentNode: AssistStructure.ViewNode): List<AutofillViewData> {
            val autofillViewData =
                if (currentNode.autofillId == null)
                    null
                else AutofillViewData(
                    autofillId = currentNode.autofillId!!,
                    autofillHints = currentNode.autofillHints?.toList() ?: emptyList(),
                    hint = currentNode.hint,
                    text = currentNode.text?.toString(),
                    isFocused = currentNode.isFocused,
                )
            return listOfNotNull(autofillViewData) +
                    (0 until currentNode.childCount).flatMap {
                        traverseStructureFromRootNodeToGetAutofillViewData(currentNode.getChildAt(it))
                    }
        }

        internal fun Context.addAuthenticationToFillResponse(
            fillResponseBuilder: FillResponse.Builder,
            dataSources: List<Credential>,
            autofillViewsData: List<AutofillViewData>,
            fillableAutofillIds: Array<AutofillId>
        ) {
            // Create intent to start authentication activity
            val authenticationIntent = Intent(this, AutofillAuthenticationActivity::class.java).apply {
                putExtra(
                    AutofillAuthenticationActivity.EXTRA_DATA_SOURCE_IDS,
                    dataSources.map { it.id }.toLongArray()
                )
                putParcelableArrayListExtra(
                    AutofillAuthenticationActivity.EXTRA_AUTOFILL_VIEW_DATA,
                    autofillViewsData.toCollection(ArrayList())
                )
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                AUTHENTICATION_REQUEST_CODE,
                authenticationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ).intentSender

            // Add authentication to fill response with a presentation
            if (Build.VERSION.SDK_INT >= 33) {
                // Create presentations
                val remoteViewsPresentation = createRemoveViewsPresentationForAuthentication()
                val presentations = Presentations.Builder()
                    .setDialogPresentation(remoteViewsPresentation)
                    .setMenuPresentation(remoteViewsPresentation)
                    .build()

                fillResponseBuilder.setAuthentication(
                    fillableAutofillIds, // specifies views that show the authentication UI -> Leaks information about what kind data is stored to unauthenticated users
                    pendingIntent,
                    presentations
                )

            } else {
                val presentation = createRemoveViewsPresentationForAuthentication()
                @Suppress("DEPRECATION")
                fillResponseBuilder.setAuthentication(
                    fillableAutofillIds, // specifies views that show the authentication UI -> Leaks information about what kind data is stored to unauthenticated users
                    pendingIntent,
                    presentation
                )
            }
        }

        /**
         * Tries to find a value for the given [AutofillViewData] using the given [Credential].
         * @return A pair of [AutofillId] and the corresponding [CredentialField], or null if no value could be found for the given node.
         */
        private fun autofillValueForNode(
            dataSource: Credential,
            viewData: AutofillViewData
        ): Pair<AutofillId, CredentialField>? {
            // Try to use the autofill hints to find a value
            val value = valueGivenAutofillHints(dataSource, viewData.autofillHints)
                ?: valueGivenHintAndText(dataSource, viewData.hint, viewData.text)
                ?: return null // no value found

            return Pair(viewData.autofillId, value)
        }

        private fun getPackageName(latestStructure: AssistStructure): String {
            return latestStructure.activityComponent.packageName
        }

        private fun getWebDomain(latestStructure: AssistStructure): String? {
            // only check the first window (out of `latestStructure.windowNodeCount` windows)
            val viewRootNode = latestStructure.getWindowNodeAt(0).rootViewNode

            fun recursivelyFindFirstWebDomain(node: AssistStructure.ViewNode): String? {
                if (node.webDomain != null && node.webDomain!!.isNotEmpty()) {
                    return node.webDomain!!
                }
                return (0 until node.childCount)
                    .firstNotNullOfOrNull {
                        recursivelyFindFirstWebDomain(node.getChildAt(it))
                    }
            }

            return recursivelyFindFirstWebDomain(viewRootNode)
        }

        private fun Context.createInlinePresentation(
            dataSource: Credential,
            credentialField: CredentialField
        ): InlinePresentation? {
            return null // TODO Not needed but would be nice
        }

        private fun Context.createInlineTooltipPresentation(
            dataSource: Credential,
            credentialField: CredentialField
        ): InlinePresentation? {
            return null // TODO Not needed but would be nice
        }

        private fun Context.createDialogPresentation(
            dataSource: Credential,
            credentialField: CredentialField
        ): RemoteViews? {
            return createRemoteViewsPresentation(dataSource, credentialField) // TODO Improve
        }

        private fun Context.createMenuPresentation(
            dataSource: Credential,
            credentialField: CredentialField
        ): RemoteViews? {
            return createRemoteViewsPresentation(dataSource, credentialField) // TODO Improve
        }

        private fun Context.createRemoteViewsPresentation(
            dataSource: Credential,
            credentialField: CredentialField
        ): RemoteViews {
            val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1)
            val value =
                if (!credentialField.secret) credentialField.value
                else getString(R.string.x_for_y).format(
                    credentialField.name,
                    dataSource.name
                ) // Don't show secret values
            presentation.setTextViewText(android.R.id.text1, value)
            return presentation
        }

        private fun Context.createRemoveViewsPresentationForAuthentication(): RemoteViews {
            return RemoteViews(packageName, R.layout.autofill_authenticate_item).apply {
                setTextViewText(
                    android.R.id.text1,
                    getString(R.string.authenticate_to_use_x_autofill).format(getString(R.string.app_name))
                )
            }
        }

    }

}