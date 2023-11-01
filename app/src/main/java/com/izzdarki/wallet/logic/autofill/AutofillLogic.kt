package com.izzdarki.wallet.logic.autofill

import android.os.Build
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import com.izzdarki.wallet.data.Credential
import com.izzdarki.wallet.data.CredentialField

/**
 * Finds the data sources that match the given request.
 *
 * @param webDomain The web domain of the request. Primarily used for finding a match.
 * @param packageName The package name of the request. Only used if `webDomain` is null.
 * @return A list of data sources that match the request.
 *  For example, if the user has multiple accounts for the same website, there will be multiple data sources.
 */
fun findDataSourcesForRequest(allCredentials: List<Credential>, webDomain: String?, packageName: String): List<Credential> {

    fun isWebDomainMatch(credential: Credential): Boolean {
        if (webDomain == null)
            return false

        // Match based on credential name
        if (credential.name
            .split(Regex("\\s+"))
            .any { word -> word.length >= 4 && webDomain.contains(word, ignoreCase = true) } // 4 letter minimum to avoid false positives
        ) return true // e.g. "accounts.google.com" is matched by "Google", but also by "Google for work"

        // Match based on credential fields
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

    return allCredentials.filter { credential ->
        // Only use package name if web domain is null (otherwise package name == browser)
        (webDomain != null && isWebDomainMatch(credential)) || (webDomain == null && isPackageNameMatch(credential))
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun valueGivenAutofillHints(dataSource: Credential, autofillHints: Collection<String>): CredentialField? {
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

fun valueGivenHintAndText(dataSource: Credential, hint: String?, text: String?): CredentialField? {
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

private fun valueForUsername(dataSource: Credential): CredentialField? {
    return dataSource.fields.find { describesUsername(it.name) }
}

private fun valueForEmail(dataSource: Credential): CredentialField? {
    // First find some value that looks like an email, if that fails, find a field that describes an email
    return dataSource.fields.find { isEmail(it.value) } ?: dataSource.fields.find { describesEmail(it.name) }
}

private fun valueForPassword(dataSource: Credential): CredentialField? {
    return dataSource.fields.find { describesPassword(it.name) }
}

private fun valueForGivenHint(dataSource: Credential, hint: String): CredentialField? {
    return dataSource.fields.find {
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
