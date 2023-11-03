package com.izzdarki.wallet.logic.autofill

import android.os.Build
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import com.izzdarki.wallet.data.Credential
import com.izzdarki.wallet.data.CredentialField
import com.izzdarki.wallet.services.AutofillViewData

enum class AutofillLogicalGroup {
    LOGIN, CREDIT_CARD, ADDRESS, OTHER
}

fun groupOf(viewData: AutofillViewData, fillValue: CredentialField): AutofillLogicalGroup {
    TODO("implement")
}

/**
 * Finds the data sources that match the given request.
 *
 * @param webDomain The web domain of the request. Primarily used for finding a match.
 * @param packageName The package name of the request. Only used if `webDomain` is null.
 * @return A list of data sources (= [Credential]) that match the request. A given data source matches the request if
 *
 *   Case [webDomain] != `null`:
 *   1. A word of [Credential.name] is contained in the [webDomain] (case insensitive)
 *      (e.g. "accounts.google.com" is matched by "Google", but also by "Google for work")
 *   2. A field of the [Credential] matches [webDomain]
 *      (e.g. webDomain = "accounts.google.com" is matched by "https://www.google.com/some/path")
 *      (checks if [webDomain] ends with the field values substring after "www." and "://", but before "/", case insensitive)
 *
 *   Case [webDomain] == `null`:
 *   1. A field of the [Credential] equals [packageName] (case insensitive)
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
        if (credential.fields.any {
                val foundWebDomain = it.value
                    .substringAfterLast("www.")
                    .substringAfterLast("://")
                    .substringBefore("/")
                webDomain.length >= 4 && '.' in webDomain
                        && webDomain.endsWith(foundWebDomain, ignoreCase = true)
            }
        ) return true

        return false
    }

    fun isPackageNameMatch(credential: Credential): Boolean {
        return credential.fields.any { it.value.equals(packageName, ignoreCase = true) }
    }

    return allCredentials.filter { credential ->
        // Only use package name if web domain is null (otherwise package name == browser)
        (webDomain != null && isWebDomainMatch(credential)) || (webDomain == null && isPackageNameMatch(credential))
    }
}

@RequiresApi(Build.VERSION_CODES.O)
/**
 * Finds the value of the given data source that matches the given autofill hints.
 * If no field matches, null is returned.
 *
 * For a supported autofill hint, a matching field is searched in the following order:
 * 1. First search a field with a value that looks like what the hint describes (e.g. email hint -> look for a field value like "example@mail.com"),
 * 2. If that fails, search for a field with a name that describes what the hint describes (e.g. email hint -> look for a field name like "email"),
 * 3. If that fails, try the next hint.
 *
 * Note that for many autofill hints one or both of steps 1 and 2 are not supported (yet).
 * @return A [CredentialField] that matches the given [autofillHints], or `null` if no match was found.
 */
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

/**
 * Finds the value of the given data source that matches the given hint and text.
 * If no field matches, null is returned.
 *
 * If the hint and text could be interpreted as describing a specific supported type of data (like email),
 * a matching field is searched in the following order:
 * 1. First search a field with a value that looks like what the hint describes (e.g. email hint -> look for a field value like "example@mail.com"),
 * 2. If that fails, search for a field with a name that describes what the hint describes (e.g. email hint -> look for a field name like "email"),
 * 3. If that fails, search for a field with a name that matches the hint exactly (e.g. hint = "custom field", look for a field name like "custom field").
 *
 * Note that for many types of data, one or both of steps 1 and 2 are not supported (yet).
 * @return A [CredentialField] that matches the given [hint] and [text], or `null` if no match was found.
 */
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

internal fun valueForUsername(dataSource: Credential): CredentialField? {
    return dataSource.fields.find { describesUsername(it.name) }
}

internal fun valueForEmail(dataSource: Credential): CredentialField? {
    // First find some value that looks like an email, if that fails, find a field that describes an email
    return dataSource.fields.find { isEmail(it.value) } ?: dataSource.fields.find { describesEmail(it.name) }
}

internal fun valueForPassword(dataSource: Credential): CredentialField? {
    return dataSource.fields.find { describesPassword(it.name) }
}

internal fun valueForGivenHint(dataSource: Credential, hint: String): CredentialField? {
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
