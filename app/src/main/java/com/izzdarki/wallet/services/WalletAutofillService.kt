package com.izzdarki.wallet.services

import android.app.assist.AssistStructure
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.util.Log
import androidx.annotation.RequiresApi
import com.izzdarki.wallet.data.CredentialField

@RequiresApi(Build.VERSION_CODES.O)
class WalletAutofillService : AutofillService() {

    override fun onCreate() {
        super.onCreate()
        // TODO Do I need to implement something here?
    }

    override fun onFillRequest(fillRequest: FillRequest, cancellationSignal: CancellationSignal, fillCallback: FillCallback) {

        // Find out where the user is trying to fill
        val fillContexts = fillRequest.fillContexts
        val latestStructure = fillContexts[fillContexts.size - 1].structure
        val webDomain = getWebDomain(latestStructure)
        val packageName = getPackageName(latestStructure)  // If webDomain is null, this is considered the package name of the browser => use webDomain

        // TODO Maybe setup the autofill service in manifest and just debug how requests look

        // TODO Implement this
    }

    override fun onSaveRequest(saveRequest: SaveRequest, saveCallback: SaveCallback) {
        // TODO Implement this
    }


    private fun getPackageName(latestStructure: AssistStructure): String {
        Log.d("autofill", "onFillRequest: packageName: ${latestStructure.activityComponent.packageName}")  // TODO remove logging
        return latestStructure.activityComponent.packageName
    }

    private fun getWebDomain(latestStructure: AssistStructure): String? {
        // only check the first window (out of `latestStructure.windowNodeCount` windows)
        val viewRootNode = latestStructure.getWindowNodeAt(0).rootViewNode

        fun structureToString(node: AssistStructure.ViewNode, level: Int = 0): String {
            val indent = "  ".repeat(level + 1)
            val type = node.className
            val webDomain = node.webDomain
            val children = (0 until node.childCount).joinToString("\n") {
                structureToString(node.getChildAt(it), level + 1)
            }
            return "$indent$type: $webDomain" +
                if (children.isNotEmpty()) "\n$children"
                else ""
        }

        fun recursivelyFindFirstWebDomain(node: AssistStructure.ViewNode): String? {
            if (node.webDomain != null && node.webDomain!!.isNotEmpty()) {
                return node.webDomain!!
            }
            return (0 until node.childCount)
                .map { recursivelyFindFirstWebDomain(node.getChildAt(it)) }
                .firstOrNull { it != null }
        }

        Log.d("autofill", "onFillRequest: first web domain ${recursivelyFindFirstWebDomain(viewRootNode)}")  // TODO remove logging

        return recursivelyFindFirstWebDomain(viewRootNode)
    }

    private fun selectAttributeMapForFillRequest(webDomain: String?, packageName: String): List<CredentialField> {
        // First try to find a match for the web domain
        // TODO Implement this
        return listOf()
    }

//    private fun autofillValueForNode(node: AssistStructure.ViewNode): Pair<AutofillId, Field> {
//
//        val autofillValue = AutofillValue.forText(valueForField(node))
//        val field = Field(autofillValue)
//        return Pair(autofillId, field)
//
//    }
//
//    private fun valueForAutofillHints(autofillHints: List<String>): String? {
//        // TODO Implement this
//    }
//
//    private fun valueForView(hint: String, text: String): String? {
//
//    }

}