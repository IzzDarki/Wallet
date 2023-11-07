package com.izzdarki.wallet.logic.autofill

import android.os.Build
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import com.izzdarki.wallet.data.Credential
import com.izzdarki.wallet.data.CredentialField

enum class AutofillLogicalGroup {
    LOGIN, PAYMENT, OTHER
}

/**
 * Determines the logical group that a given [CredentialField] belongs to.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun groupOf(fillValue: CredentialField): AutofillLogicalGroup {
    // Only the value that is filled is relevant to the determine the group
    // The field it is filled into is not controlled by this app and is therefore not used to determine the group
    when {
        isEmail(fillValue.value)
        || describesEmail(fillValue.name)
        || describesUsername(fillValue.name)
        || describesPassword(fillValue.name)
        -> return AutofillLogicalGroup.LOGIN

        isCreditCardNumber(fillValue.value)
        || isIBAN(fillValue.value)
        || isBIC(fillValue.value)
        || describesCardNumber(fillValue.name)
        || describesIBAN(fillValue.name)
        || describesBIC(fillValue.name)
        || describesCardSecurityCode(fillValue.name)
        -> return AutofillLogicalGroup.PAYMENT

        else -> return AutofillLogicalGroup.OTHER
    }
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

                (webDomain.length >= 4 && '.' in webDomain
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

//@RequiresApi(Build.VERSION_CODES.O)
//private fun isCreditCardNumberAutofillHint(hint: String): Boolean {
//    return hint == View.AUTOFILL_HINT_CREDIT_CARD_NUMBER
//            || hint == W3C_CREDIT_CARD_NUMBER
//}
//
//@RequiresApi(Build.VERSION_CODES.O)
//private fun isCreditCardExpirationDateAutofillHint(hint: String): Boolean {
//    return hint == View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE
//            || hint == W3C_CREDIT_CARD_EXP
//}
//
//@RequiresApi(Build.VERSION_CODES.O)
//private fun isCreditCardExpirationYearAutofillHint(hint: String): Boolean {
//    return hint == View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR
//            || hint == W3C_CREDIT_CARD_EXP_YEAR
//}
//
//@RequiresApi(Build.VERSION_CODES.O)
//private fun isCreditCardExpirationMonthAutofillHint(hint: String): Boolean {
//    return hint == View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH
//            || hint == W3C_CREDIT_CARD_EXP_MONTH
//}
//
//@RequiresApi(Build.VERSION_CODES.O)
//private fun isCreditCardExpirationDayAutofillHint(hint: String): Boolean {
//    return hint == View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY
//}
//
//@RequiresApi(Build.VERSION_CODES.O)
//private fun isCreditCardSecurityCodeAutofillHint(hint: String): Boolean {
//    return hint == View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE
//            || hint == W3C_CREDIT_CARD_CSC
//}


// According to https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#autofilling-form-controls%3A-the-autocomplete-attribute
// (Link found on https://developer.android.com/guide/topics/text/autofill-services)
// the following hints are more likely some views (probably on web pages)
const val W3C_USERNAME_HINT = "username"
const val W3C_CURRENT_PASSWORD_HINT = "current-password"
const val W3C_EMAIL_HINT = "email"
//const val W3C_CREDIT_CARD_NAME = "cc-name"
//const val W3C_CREDIT_GIVEN_NAME = "cc-given-name"
//const val W3C_CREDIT_ADDITIONAL_NAME = "cc-additional-name"
//const val W3C_CREDIT_FAMILY_NAME = "cc-family-name"
//const val W3C_CREDIT_CARD_NUMBER = "cc-number"
//const val W3C_CREDIT_CARD_EXP = "cc-exp"
//const val W3C_CREDIT_CARD_EXP_MONTH = "cc-exp-month"
//const val W3C_CREDIT_CARD_EXP_YEAR = "cc-exp-year"
//const val W3C_CREDIT_CARD_CSC = "cc-csc"
//const val W3C_CREDIT_CARD_TYPE = "cc-type"
//const val W3C_STREET_ADDRESS = "street-address"
//const val W3C_ADDRESS_LINE1 = "address-line1"
//const val W3C_ADDRESS_LINE2 = "address-line2"
//const val W3C_ADDRESS_LINE3 = "address-line3"
//const val W3C_ADDRESS_LEVEL4 = "address-level4"
//const val W3C_ADDRESS_LEVEL3 = "address-level3"
//const val W3C_ADDRESS_LEVEL2 = "address-level2"
//const val W3C_ADDRESS_LEVEL1 = "address-level1"
//const val W3C_COUNTRY = "country"
//const val W3C_COUNTRY_NAME = "country-name"
//const val W3C_POSTAL_CODE = "postal-code"
