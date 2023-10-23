package com.izzdarki.wallet.services

import android.app.assist.AssistStructure
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
import izzdarki.wallet.R

@RequiresApi(Build.VERSION_CODES.O)
class WalletAutofillService : AutofillService() {

    override fun onFillRequest(fillRequest: FillRequest, cancellationSignal: CancellationSignal, fillCallback: FillCallback) {

        // Find out where the user is trying to fill
        val fillContexts = fillRequest.fillContexts
        val latestStructure = fillContexts[fillContexts.size - 1].structure
        val webDomain = getWebDomain(latestStructure)
        val packageName = getPackageName(latestStructure)  // If webDomain is not null, this is considered the package name of the browser => use webDomain
        Log.d("autofill", "Fill request for web domain $webDomain and package name $packageName")

        // Find the data sources that match the request (more than one if the user has multiple accounts for the same website)
        val suitableDataSources = findDataSourcesForRequest(
            allCredentials = CredentialPreferenceStorage.readAllCredentials(this),
            webDomain,
            packageName
        )
        // One dataset per data source
        val datasets = suitableDataSources.mapNotNull { dataSource ->
            // Retrieve pairs of autofill ids and corresponding values for the given structure
            val autofillValues = traverseStructureToGetAutofillValues(dataSource, latestStructure.getWindowNodeAt(0).rootViewNode)
            createDataset(dataSource, autofillValues)
        }

        if (datasets.isEmpty()) { // No suitable data sources (that also generate a dataset) were found
            fillCallback.onSuccess(null) // nothing to fill
            Log.d("autofill", "No suitable data sources found")
            return
        }

        val fillResponseBuilder = FillResponse.Builder()
        for (dataset in datasets)
            fillResponseBuilder.addDataset(dataset)
        fillCallback.onSuccess(fillResponseBuilder.build())
    }

    override fun onSaveRequest(saveRequest: SaveRequest, saveCallback: SaveCallback) {
        // TODO Implement this
    }

    /**
     * Traverses the structure to find a mapping of [AutofillId]s to their corresponding values,
     * using the given [DataSource].
     */
    private fun traverseStructureToGetAutofillValues(dataSource: DataSource, currentNode: AssistStructure.ViewNode): List<Pair<AutofillId, CredentialField>> {
        return listOfNotNull(autofillValueForNode(dataSource, currentNode)) +
                (0 until currentNode.childCount).flatMap {
                    traverseStructureToGetAutofillValues(dataSource, currentNode.getChildAt(it))
                }
    }

    /**
     * Tries to find a value for the given [AssistStructure.ViewNode] using the given [DataSource].
     * @return A pair of [AutofillId] and the corresponding [CredentialField], or null if no value could be found for the given node.
     */
    private fun autofillValueForNode(dataSource: DataSource, node: AssistStructure.ViewNode): Pair<AutofillId, CredentialField>? {
        if (node.autofillId == null)
            return null // nodes without autofill ids are ignored

        // Try to use the autofill hints to find a value
        val value = valueGivenAutofillHints(dataSource, node.autofillHints ?: arrayOf())
            .apply { if (this != null) Log.d("autofill", "Found value ${this.value} for autofillHints = ${node.autofillHints?.joinToString(", ") ?: "none"}") } // TODO remove logging
            ?: valueGivenHintAndText(dataSource, node.hint, node.text?.toString())
            .apply { if (this != null) Log.d("autofill", "Found value ${this.value} for hint = ${node.hint}, text = ${node.text}") } // TODO remove logging
            ?: return null // no value found

        return Pair(node.autofillId!!, value)
    }

    /**
     * Creates a [Dataset] containing all the given `autofillValues`
     * @param dataSource Data source that was used to find the values
     * @param autofillValues Mapping of [AutofillId]s to the [CredentialField]s, that should be used to fill them
     */
    private fun createDataset(dataSource: DataSource, autofillValues: List<Pair<AutofillId, CredentialField>>): Dataset? {
        if (autofillValues.isEmpty())
            return null // nothing to fill

        val datasetBuilder = Dataset.Builder()

        // TODO Authentication

        for ((autofillId, credentialField) in autofillValues) {
            if (Build.VERSION.SDK_INT >= 33) {
                // Create presentations
                val presentationsBuilder = Presentations.Builder()
                createInlinePresentation(dataSource, credentialField)?.let { presentationsBuilder.setInlinePresentation(it) }
                createInlineTooltipPresentation(dataSource, credentialField)?.let { presentationsBuilder.setInlineTooltipPresentation(it) }
                createDialogPresentation(dataSource, credentialField)?.let { presentationsBuilder.setDialogPresentation(it) }
                createMenuPresentation(dataSource, credentialField)?.let { presentationsBuilder.setMenuPresentation(it) }

                // Create field
                val field = Field.Builder() // Only available in API 33+
                    .setValue(AutofillValue.forText(credentialField.value))
                    .setPresentations(presentationsBuilder.build())
                    .build()

                // Add field to dataset
                datasetBuilder.setField(autofillId, field)
            } else {
                datasetBuilder.setValue(
                    autofillId,
                    AutofillValue.forText(credentialField.value),
                    createRemoteViewsPresentation(dataSource, credentialField)
                )
            }
        }
        return datasetBuilder.build()
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
                .firstNotNullOfOrNull { recursivelyFindFirstWebDomain(node.getChildAt(it))
                }
        }

        return recursivelyFindFirstWebDomain(viewRootNode)
    }

    private fun createInlinePresentation(dataSource: DataSource, credentialField: CredentialField): InlinePresentation? {
        return null // TODO Not needed but would be nice
    }

    private fun createInlineTooltipPresentation(dataSource: DataSource, credentialField: CredentialField): InlinePresentation? {
        return null // TODO Not needed but would be nice
    }

    private fun createDialogPresentation(dataSource: DataSource, credentialField: CredentialField): RemoteViews? {
        return createRemoteViewsPresentation(dataSource, credentialField) // TODO Improve
    }

    private fun createMenuPresentation(dataSource: DataSource, credentialField: CredentialField): RemoteViews? {
        return createRemoteViewsPresentation(dataSource, credentialField) // TODO Improve
    }

    private fun createRemoteViewsPresentation(dataSource: DataSource, credentialField: CredentialField): RemoteViews {
        val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1)
        val value =
            if (!credentialField.secret) credentialField.value
            else getString(R.string.x_for_y).format(credentialField.name, dataSource.name) // Don't show secret values
        presentation.setTextViewText(android.R.id.text1, value)
        return presentation
    }

}