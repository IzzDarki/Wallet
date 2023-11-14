@file:RequiresApi(Build.VERSION_CODES.O)
package com.izzdarki.wallet.logic.autofill

import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import com.izzdarki.wallet.data.Credential
import com.izzdarki.wallet.data.CredentialField
import com.izzdarki.wallet.services.AutofillViewData

enum class AutofillLogicalGroup {
    LOGIN, PAYMENT, OTHER
}

/**
 * Heuristics to find out if a field or hint/text is of a certain type of field (e.g. password)
 * Is used to determine the type of field that can be filled in an [AutofillViewData]
 * and then later to determine the correct [CredentialField] of a [Credential] to be used for filling
 */
class FieldTypeHeuristic(
    /** If not null, this function is used to determine if a value is of this type */
    val isType: ((value: String) -> Boolean)? = null,

    /** If not null, this function is used to determine if a name describes this type */
    val describesType: (name: String) -> Boolean,

    /** The logical group that this type belongs to */
    val logicalGroup: AutofillLogicalGroup,

    /** The autofill hints associated with this type */
    val autofillHints: List<String>,
) {

    /**
     * Tries to find a matching field in the given [Credential] and returns it.
     * 1. First tries to find a match by value (using [isType]) (e.g. email heuristic -> look for a field value like "example@mail.com"),
     * 2. If there is no field that matches by value, tries to find a match by name (using [describesType]) (e.g. email heuristic -> look for a field name like "email"),
     * @return The matching field or `null` if none was found
     */
    fun findMatchingField(dataSource: Credential): CredentialField? {
        val matchedByValue = if (isType == null) null
            else dataSource.fields.find { isType.invoke(it.value) }
        return matchedByValue ?: dataSource.fields.find { describesType(it.name) }
    }
}

internal val emailHeuristic = FieldTypeHeuristic(
    isType = { value: String ->
        value.matches(EMAIL_REGEX)
    },
    describesType = { name: String ->
        name.lowercase().withoutWhitespaceOrDashes() in listOf(
           "email", "mail", "emailaddress", "mailaddress", // english specific
            "emailadresse ", "mailadresse", // german specific
        )
    },
    logicalGroup = AutofillLogicalGroup.LOGIN,
    autofillHints = listOf(View.AUTOFILL_HINT_EMAIL_ADDRESS, W3C_EMAIL_HINT)
)

internal val usernameHeuristic = FieldTypeHeuristic(
    isType = null,
    describesType = { name: String ->
        name.lowercase().withoutWhitespaceOrDashes() in listOf(
            "username", "user", "login", "account", // english specific
            "benutzername", "benutzer", "konto", // german specific
        )
    },
    logicalGroup = AutofillLogicalGroup.LOGIN,
    autofillHints = listOf(View.AUTOFILL_HINT_USERNAME, W3C_USERNAME_HINT)
)

internal val passwordHeuristic = FieldTypeHeuristic(
    isType = null,
    describesType = { name: String ->
        name.lowercase().withoutWhitespaceOrDashes() in listOf(
            "pin", "pw", // general
            "password", // english specific
            "passwort", "kennwort", // german specific
        )
    },
    logicalGroup = AutofillLogicalGroup.LOGIN,
    autofillHints = listOf(View.AUTOFILL_HINT_PASSWORD, W3C_CURRENT_PASSWORD_HINT)
)

internal val phoneHeuristic = FieldTypeHeuristic(
    isType = null,
    describesType = { name: String ->
        name.lowercase().withoutWhitespaceOrDashes() in listOf(
            "tel", // general
            "phone", "telephone", "phonenumber", "telephonenumber", "mobilenumber", "mobilephone", // english specific
            "telefon", "telefonnummer", "mobiltelefon", "mobiltelefonnummer", "handy", "handynummer", // german specific
        )
    },
    logicalGroup = AutofillLogicalGroup.OTHER,
    autofillHints = listOf(View.AUTOFILL_HINT_PHONE, W3C_TELEPHONE_HINT)
)

internal val creditCardNumberHeuristic = FieldTypeHeuristic(
    isType = { value: String ->
        val withoutSpaces = value.withoutWhitespace()
        if (withoutSpaces.length < 12 || withoutSpaces.length > 19)
            return@FieldTypeHeuristic false
        return@FieldTypeHeuristic passesLuhnCheck(withoutSpaces)
    },
    describesType = { name: String ->
        name.lowercase().withoutWhitespaceOrDashes() in listOf(
            "creditcardnumber", "cardnumber", // english specific
            "kreditkartennummer", "kartennummer", "nummerderkreditkarte" // german specific
        )
    },
    logicalGroup = AutofillLogicalGroup.PAYMENT,
    autofillHints = listOf(View.AUTOFILL_HINT_CREDIT_CARD_NUMBER, W3C_CREDIT_CARD_NUMBER)
)

internal val creditCardSecurityCodeHeuristic = FieldTypeHeuristic(
    isType = null,
    describesType = { name: String ->
        // From https://www.sparkasse.de/pk/ratgeber/finanzglossar/kartenpruefnummer.html
        name.lowercase().withoutWhitespaceOrDashes() in listOf(
            "cardverificationvalue", "cvv", "cardverificationnumber", "cvn", // general
            "cardsecuritycode", "csc", "cardcodeverification", "cvccode", "cvvcode" // general
        )
    },
    logicalGroup = AutofillLogicalGroup.PAYMENT,
    autofillHints = listOf(View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE, W3C_CREDIT_CARD_CSC)
)

internal val ibanHeuristic = FieldTypeHeuristic(
    isType = { value: String ->
        // https://www.iban.com/glossary: 34 hard upper bound
        // https://en.wikipedia.org/wiki/International_Bank_Account_Number#Validating_the_IBAN: Heuristics
        val withoutSpaces = value.withoutWhitespace()
        if (withoutSpaces.length < 10 || withoutSpaces.length > 34)
            return@FieldTypeHeuristic false
        return@FieldTypeHeuristic withoutSpaces[0].isLetter() && withoutSpaces[1].isLetter()
                && withoutSpaces[2].isDigit() && withoutSpaces[3].isDigit()
                && withoutSpaces.subSequence(4, withoutSpaces.length).all { it.isLetterOrDigit() }
    },
    describesType = { name: String ->
        name.lowercase().withoutWhitespaceOrDashes() in listOf(
            "iban", // general
            "internationalbankaccountnumber", // english specific
            "internationalebankkontonummer", // german specific
        )
    },
    logicalGroup = AutofillLogicalGroup.PAYMENT,
    autofillHints = listOf()
)

internal val bicHeuristic = FieldTypeHeuristic(
    isType = { value: String ->
        // https://www.iban.com/glossary: Length is 8 or 11
        val withoutSpaces = value.withoutWhitespace()
        if (withoutSpaces.length !in listOf(8, 11))
            return@FieldTypeHeuristic false
        return@FieldTypeHeuristic withoutSpaces.toCharArray().all { it.isLetterOrDigit() }
    },
    describesType = { name: String ->
        name.lowercase().withoutWhitespaceOrDashes() in listOf(
            "bic", "swiftaddress", "swiftcode"
        )
    },
    logicalGroup = AutofillLogicalGroup.PAYMENT,
    autofillHints = listOf()
)

// Since this is traversed top to bottom, it should be ordered such that good working, specific heuristics are first
internal val fieldTypeHeuristics = listOf(
    emailHeuristic,
    usernameHeuristic,
    passwordHeuristic,
    phoneHeuristic,
    creditCardNumberHeuristic,
    creditCardSecurityCodeHeuristic,
    ibanHeuristic, // isType might have a significant false positive rate
    bicHeuristic, // isType might have a significant false positive rate
)


/** Used to find out if a credential should be used to fill a request from a specific website */
internal fun describesUrl(fieldName: String) = fieldName.lowercase().withoutWhitespaceOrDashes() in listOf(
    "url", "link", "homepage", // general
    "website", "webpage", "site", // english specific
    "webseite", "seite", "internetseite", // german specific
)

/** Used to find out if a credential should be used to fill a request from a specific app */
internal fun describesApp(fieldName: String) = fieldName.lowercase().withoutWhitespaceOrDashes() in listOf(
    "app", // general
    "application", // english specific
    "anwendung", // german specific
)

internal val EMAIL_REGEX: Regex by lazy { Regex("^[\\w-.%+-]+@([\\w-]+\\.)+[\\w-]{2,6}$") }

internal fun String.withoutWhitespaceOrDashes() = this.replace(Regex("[\\s-]+"), "")
internal fun String.withoutWhitespace() = this.replace(Regex("\\s+"), "")
