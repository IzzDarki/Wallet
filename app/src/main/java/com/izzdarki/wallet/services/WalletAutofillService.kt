package com.izzdarki.wallet.services

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
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
import com.izzdarki.wallet.data.CredentialField
import com.izzdarki.wallet.logic.autofill.DataSource
import com.izzdarki.wallet.logic.autofill.findDataSourcesForRequest
import com.izzdarki.wallet.logic.autofill.valueGivenAutofillHints
import com.izzdarki.wallet.logic.autofill.valueGivenHintAndText
import com.izzdarki.wallet.storage.CredentialPreferenceStorage
import com.izzdarki.wallet.ui.authentication.AutofillAuthenticationActivity
import izzdarki.wallet.R
import java.io.Serializable

/**
 * Captures all information about a view that is eligible for autofill.
 */
data class AutofillViewData(
    val autofillId: AutofillId,
    val autofillHints: Collection<String>,
    val hint: String?,
    val text: String?,
) : Serializable

@RequiresApi(Build.VERSION_CODES.O)
class WalletAutofillService : AutofillService() {

    override fun onFillRequest(fillRequest: FillRequest, cancellationSignal: CancellationSignal, fillCallback: FillCallback) {

        // Find out where the user is trying to fill
        val fillContexts = fillRequest.fillContexts
        val latestStructure = fillContexts[fillContexts.size - 1].structure
        val webDomain = getWebDomain(latestStructure)
        val packageName =
            getPackageName(latestStructure)  // If webDomain is not null, this is considered the package name of the browser => use webDomain
        Log.d("autofill", "Fill request for web domain $webDomain and package name $packageName")

        // Find the data sources that match the request (more than one if the user has multiple accounts for the same website)
        val suitableDataSources = findDataSourcesForRequest(
            allCredentials = CredentialPreferenceStorage.readAllCredentials(this),
            webDomain,
            packageName
        )

        // Find all autofill ids that can be filled using the given data sources
        val autofillViewsData = traverseStructureToGetAutofillViewData(latestStructure)
        val fillableAutofillIds = suitableDataSources.flatMap { dataSource ->
            getAutofillValues(dataSource, autofillViewsData).map { it.first }
            // This already determines what to fill for each view, but then only remembers the autofill id
            // The actual filling is done (again) after the user authenticated
        }



        if (fillableAutofillIds.isEmpty()) { // No views can be filled
            fillCallback.onSuccess(null) // null means nothing can be filled
            Log.d("autofill", "No suitable data sources found")
            return
        }


        // TODO Authentication
        //      In this scenario, the user can see nothing before authenticating (just a box saying "Authenticate to use Autofill")
        //      Implementation: No datasets need to be generated here (but we need the autofill ids)
        //                      The AuthenticationActivity then creates all datasets (essentially doing what is currently done here)


        // Create fill response without any data, just for the authentication
        // Still this leaks information about what kind of data is stored to unauthenticated users, because an unauthenticated user can see what views can be filled
        val fillResponseBuilder = FillResponse.Builder()
        addAuthenticationToFillResponse(fillResponseBuilder, autofillViewsData, fillableAutofillIds.toTypedArray())

        fillCallback.onSuccess(fillResponseBuilder.build())

        // One dataset per data source
//        val datasets = suitableDataSources.mapNotNull { dataSource ->
//            // Retrieve pairs of autofill ids and corresponding values for the given structure
//            val autofillValues = traverseStructureToGetAutofillValues(dataSource, latestStructure)
//            createDataset(dataSource, autofillValues) // null if for all views no value could be found using this data source
//        }
        // Add all datasets to the response
//        for (dataset in datasets)
//            fillResponseBuilder.addDataset(dataset)
    }

    override fun onSaveRequest(saveRequest: SaveRequest, saveCallback: SaveCallback) {
        // TODO Implement this
    }

    companion object {

        private const val AUTHENTICATION_REQUEST_CODE = 417023

        /**
         * Creates a [Dataset] containing all the given `autofillValues`
         * @param dataSource Data source that was used to find the values
         * @param autofillValues Mapping of [AutofillId]s to the [CredentialField]s, that should be used to fill them
         */
        fun Context.createDataset(
            dataSource: DataSource,
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
         * Tries to find a value to be filled for the given [AutofillViewData]s using the given [DataSource].
         * @return A list of [AutofillId]s and the corresponding [CredentialField]s. AutofillIds that could not be filled are not included.
         */
        private fun getAutofillValues(
            dataSource: DataSource,
            viewsData: List<AutofillViewData>
        ): List<Pair<AutofillId, CredentialField>> {
            return viewsData.mapNotNull { autofillValueForNode(dataSource, it) }
        }

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
                    text = currentNode.text?.toString()
                )
            return listOfNotNull(autofillViewData) +
                    (0 until currentNode.childCount).flatMap {
                        traverseStructureFromRootNodeToGetAutofillViewData(currentNode.getChildAt(it))
                    }
        }

        private fun Context.addAuthenticationToFillResponse(
            fillResponseBuilder: FillResponse.Builder,
            autofillViewsData: List<AutofillViewData>,
            fillableAutofillIds: Array<AutofillId>
        ) {
            // Create intent to start authentication activity
            val authenticationIntent = Intent(this, AutofillAuthenticationActivity::class.java).apply {
                putExtra(
                    AutofillAuthenticationActivity.EXTRA_AUTOFILL_VIEW_DATA,
                    autofillViewsData.toTypedArray() as Serializable
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
         * Tries to find a value for the given [AutofillViewData] using the given [DataSource].
         * @return A pair of [AutofillId] and the corresponding [CredentialField], or null if no value could be found for the given node.
         */
        private fun autofillValueForNode(
            dataSource: DataSource,
            viewData: AutofillViewData
        ): Pair<AutofillId, CredentialField>? {
            // Try to use the autofill hints to find a value
            val value = valueGivenAutofillHints(dataSource, viewData.autofillHints)
                .apply {
                    if (this != null) Log.d(
                        "autofill",
                        "Found value ${this.value} for autofillHints = ${
                            viewData.autofillHints.joinToString(", ")
                        }"
                    )
                } // TODO remove logging
                ?: valueGivenHintAndText(dataSource, viewData.hint, viewData.text)
                    .apply {
                        if (this != null) Log.d(
                            "autofill",
                            "Found value ${this.value} for hint = ${viewData.hint}, text = ${viewData.text}"
                        )
                    } // TODO remove logging
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
            dataSource: DataSource,
            credentialField: CredentialField
        ): InlinePresentation? {
            return null // TODO Not needed but would be nice
        }

        private fun Context.createInlineTooltipPresentation(
            dataSource: DataSource,
            credentialField: CredentialField
        ): InlinePresentation? {
            return null // TODO Not needed but would be nice
        }

        private fun Context.createDialogPresentation(
            dataSource: DataSource,
            credentialField: CredentialField
        ): RemoteViews? {
            return createRemoteViewsPresentation(dataSource, credentialField) // TODO Improve
        }

        private fun Context.createMenuPresentation(
            dataSource: DataSource,
            credentialField: CredentialField
        ): RemoteViews? {
            return createRemoteViewsPresentation(dataSource, credentialField) // TODO Improve
        }

        private fun Context.createRemoteViewsPresentation(
            dataSource: DataSource,
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