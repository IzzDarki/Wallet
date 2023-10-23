package com.izzdarki.wallet.logic.autofill

import android.os.Build
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import com.izzdarki.wallet.data.Credential
import com.izzdarki.wallet.data.CredentialField

/**
 * A data source is a list of [CredentialField]s, that can be used to fill a request.
 */
data class DataSource(
    val name: String,
    val data: List<CredentialField>,
)

/**
 * Finds the data sources that match the given request.
 *
 * @param webDomain The web domain of the request. Primarily used for finding a match.
 * @param packageName The package name of the request. Only used if `webDomain` is null.
 * @return A list of data sources that match the request.
 *  For example, if the user has multiple accounts for the same website, there will be multiple data sources.
 */
fun findDataSourcesForRequest(allCredentials: List<Credential>, webDomain: String?, packageName: String): List<DataSource> {

    fun isWebDomainMatch(credential: Credential): Boolean {
        if (webDomain == null)
            return false
        val webDomainProperties = credential.fields.filter { isWebDomain(it.value) } + // value is a web domain (probably works better than name)
                credential.fields.filter { describesWebDomain(it.name) } // name describes a web domain
        return webDomainProperties.any {
            val foundWebDomain = it.value.substringAfterLast("://").substringBefore("/")
            webDomain.endsWith(foundWebDomain, ignoreCase = true)
        }
    }

    fun isPackageNameMatch(credential: Credential): Boolean {
        val packageNameProperty = credential.fields.find { describesAndroidPackage(it.name) }
        return packageNameProperty?.value == packageName
    }

    return allCredentials.mapNotNull { credential ->
        // Only use package name if web domain is null (otherwise package name == browser)
        if ((webDomain != null && isWebDomainMatch(credential)) || (webDomain == null && isPackageNameMatch(credential)))
            DataSource(
                name = credential.name,
                data = credential.apply { Log.d("autofill", "Found data source $name because of ${ if(webDomain != null && isWebDomainMatch(credential)) "web domain match" else "package name match"}") }.fields // TODO Remove logging
            )
        else
            null
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun valueGivenAutofillHints(dataSource: DataSource, autofillHints: Array<String>): CredentialField? {
    // Go through all hints individually
    return autofillHints.map { hint ->
        when {
            isEmailAutofillHint(hint) -> valueForEmail(dataSource)
            isUsernameAutofillHint(hint) -> valueForUsername(dataSource)
            isPasswordAutofillHint(hint) -> valueForPassword(dataSource)
            else -> null // Could not find value using autofill hints
        }
    }.firstOrNull()
}

fun valueGivenHintAndText(dataSource: DataSource, hint: String?, text: String?): CredentialField? {
    if (hint != null && hint != "" || text != null && text != "") Log.d("autofill", "Looking for hint = $hint, text = $text")
    val hintNonNull = hint ?: "" // empty strings don't describe anything
    val textNonNull = text ?: ""
    return when {
        isEmail(textNonNull) || describesEmail(hintNonNull) -> // Look for an email
            valueForEmail(dataSource)
        describesUsername(hintNonNull) -> valueForUsername(dataSource)
        describesPassword(hintNonNull) -> valueForPassword(dataSource)
        else -> valueForGivenHint(dataSource, hintNonNull)
    }
}

private fun valueForUsername(dataSource: DataSource): CredentialField? {
    return dataSource.data.find { describesUsername(it.name) }
}

private fun valueForEmail(dataSource: DataSource): CredentialField? {
    // First find some value that looks like an email, if that fails, find a field that describes an email
    return dataSource.data.find { isEmail(it.value) } ?: dataSource.data.find { describesEmail(it.name) }
}

private fun valueForPassword(dataSource: DataSource): CredentialField? {
    return dataSource.data.find { describesPassword(it.name) }
}

private fun valueForGivenHint(dataSource: DataSource, hint: String): CredentialField? {
    return dataSource.data.find {
        it.name.withoutWhitespaceOrDashes().lowercase() == hint.withoutWhitespaceOrDashes().lowercase()
    }
}


@RequiresApi(Build.VERSION_CODES.O)
private fun isUsernameAutofillHint(hint: String): Boolean {
    return hint == View.AUTOFILL_HINT_USERNAME
            || hint == W3C_USERNAME_HINT
}

@RequiresApi(Build.VERSION_CODES.O)
private fun isEmailAutofillHint(hint: String): Boolean {
    return hint == View.AUTOFILL_HINT_EMAIL_ADDRESS
            || hint == W3C_EMAIL_HINT
}

@RequiresApi(Build.VERSION_CODES.O)
private fun isPasswordAutofillHint(hint: String): Boolean {
    return hint == View.AUTOFILL_HINT_PASSWORD
            || hint == W3C_CURRENT_PASSWORD_HINT
}

// According to https://developer.android.com/guide/topics/text/autofill-services
// the following hints are more likely some views (probably on web pages)
const val W3C_USERNAME_HINT = "username"
const val W3C_CURRENT_PASSWORD_HINT = "current-password"
const val W3C_EMAIL_HINT = "email"
