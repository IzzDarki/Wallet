@file:RequiresApi(Build.VERSION_CODES.O)
package com.izzdarki.wallet.logic.autofill

import android.os.Build
import androidx.annotation.RequiresApi
import com.izzdarki.wallet.data.Credential
import com.izzdarki.wallet.data.CredentialField

/**
 * Determines the logical group that a given [CredentialField] belongs to.
 */
fun groupOf(fillValue: CredentialField): AutofillLogicalGroup {
    // Only the value that is filled is relevant to the determine the group
    // The field it is filled into is not controlled by this app and is therefore not used to determine the group

    // Find matching field type heuristic and return its logical group
    return fieldTypeHeuristics.firstOrNull { heuristic ->
                heuristic.isType?.invoke(fillValue.value) ?: false
            }?.logicalGroup
        ?: fieldTypeHeuristics.firstOrNull { heuristic ->
                heuristic.describesType(fillValue.name)
            }?.logicalGroup
        ?: AutofillLogicalGroup.OTHER
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

                (foundWebDomain.length >= 4 && foundWebDomain.contains('.')
                    && webDomain.endsWith(foundWebDomain, ignoreCase = true) )
                || (it.value == "*" && describesUrl(it.name))
            }
        ) return true

        return false
    }

    fun isPackageNameMatch(credential: Credential): Boolean {
        return credential.fields.any {
            it.value.equals(packageName, ignoreCase = true)
            || (it.value == "*" && describesApp(it.name))
        }
    }

    return allCredentials.filter { credential ->
        // Only use package name if web domain is null (otherwise package name == browser)
        ((webDomain != null && isWebDomainMatch(credential)) || (webDomain == null && isPackageNameMatch(credential)))
                && "no-autofill" !in credential.labels
    }
}

@RequiresApi(Build.VERSION_CODES.O)
/**
 * Finds the value of the given data source that matches the given autofill hints.
 * @return A [CredentialField] that matches the given [autofillHints], or `null` if no match was found.
 */
fun valueGivenAutofillHints(dataSource: Credential, autofillHints: Collection<String>): CredentialField? {
    return fieldTypeHeuristics.firstNotNullOfOrNull { heuristic ->
        if (autofillHints.none { hint -> heuristic.autofillHints.contains(hint) })
            null // Heuristic does not belong to the requested hints
        else
            heuristic.findMatchingField(dataSource)
    }
}

/**
 * Finds the value of the given data source that matches the given hint and text.
 * If no field matches, null is returned.
 *
 * 1. Try to find out what type of data is requested from the hint/text (e.g. email, password, ...) and try to find a matching [CredentialField]
 * 2. If that fails, search for a field with a name that matches a word of the hint directly (e.g. hint = "Discord nickname" matched by field name = "nickname")
 *
 * @return A [CredentialField] that matches the given [hint] and [text], or `null` if no match was found.
 */
fun valueGivenHintAndText(dataSource: Credential, hint: String?, text: String?): CredentialField? {
    // list of the hint itself followed by all words of the hint
    val hintWords = (hint?.let {
        listOf(it) + it.split(Regex("\\s+"))
    } ?: emptyList()).filter { it.length >= 2 }

    // list of the text itself followed by all words of the text
    val textWords = (text?.let {
        listOf(it) + it.split(Regex("\\s+"))
    } ?: emptyList()).filter { it.length >= 2 }

    // Try to match using heuristics
    val matchingHeuristic = fieldTypeHeuristics.firstOrNull { heuristic ->
        // Find matching value
        if (heuristic.isType != null)
            textWords.any { heuristic.isType.invoke(it) ?: false }
        else
            false
    } ?: fieldTypeHeuristics.firstOrNull { heuristic ->
        // Find matching hint
        hintWords.any { heuristic.describesType(it) }
    } ?: fieldTypeHeuristics.firstOrNull { heuristic ->
        // If text is used as hint, try to find matching hint
        textWords.any { heuristic.describesType(it) }
    }

    val fieldUsingHeuristic = matchingHeuristic?.findMatchingField(dataSource)
    if (fieldUsingHeuristic != null)
        return fieldUsingHeuristic

    // Try to find direct match of a word of the hint and fieldName
    return hintWords.firstNotNullOfOrNull { hintWord ->
        valueForGivenHint(dataSource, hintWord)
    }
}

internal fun valueForGivenHint(dataSource: Credential, hintWord: String): CredentialField? {
    // hintWord is a word (>= 3 symbols) of the hint
    // Field name should not be split into words (e.g. hint = "Discord nickname", field name = "Discord password",
    //    if field name is split into words, "Facebook" would cause a match, although the field name does not describe the hint)
    return dataSource.fields.find {
        it.name.withoutWhitespaceOrDashes().lowercase() == hintWord.withoutWhitespaceOrDashes().lowercase()
    }
}
