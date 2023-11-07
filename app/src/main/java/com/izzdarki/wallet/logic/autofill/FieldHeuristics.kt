package com.izzdarki.wallet.logic.autofill

fun describesUsername(fieldName: String): Boolean {
    return fieldName.lowercase().withoutWhitespaceOrDashes() in listOf(
        "username", "user", "login", "account", // english specific
        "benutzername", "benutzer", "konto", // german specific
    ) || describesEmail(fieldName)
}

fun describesPassword(fieldName: String): Boolean {
    return fieldName.lowercase().withoutWhitespaceOrDashes() in listOf(
        "pin", // general
        "password", // english specific
        "passwort", "kennwort", // german specific
    )
}

fun describesEmail(fieldName: String): Boolean {
    return fieldName.lowercase().withoutWhitespaceOrDashes() in listOf(
        "email", "mail", "emailaddress", "mailaddress", // english specific
        "emailadresse ", "mailadresse", // german specific
    )
}

fun isEmail(string: String): Boolean {
    return string.matches(EMAIL_REGEX)
}

fun isCreditCardNumber(string: String): Boolean {
    val withoutSpaces = string.withoutWhitespace()
    if (withoutSpaces.length < 12 || withoutSpaces.length > 19) return false
    return passesLuhnCheck(withoutSpaces)
}


fun describesCardSecurityCode(fieldName: String): Boolean {
    // From https://www.sparkasse.de/pk/ratgeber/finanzglossar/kartenpruefnummer.html
    return fieldName.lowercase().withoutWhitespaceOrDashes() in listOf(
        "cardverificationvalue", "cvv", "cardverificationnumber", "cvn", // general
        "cardsecuritycode", "csc", "cardcodeverification", "cvccode", "cvvcode" // general
    )
}

fun describesCardNumber(fieldName: String) = fieldName.lowercase().withoutWhitespaceOrDashes() in listOf(
    "creditcardnumber", "cardnumber", // english specific
    "kreditkartennummer", "kartennummer", "nummerderkreditkarte" // german specific
)

fun describesIBAN(string: String) = string.lowercase().withoutWhitespaceOrDashes() == "iban"

fun isIBAN(string: String): Boolean {
    // https://www.iban.com/glossary: 34 hard upper bound
    // https://en.wikipedia.org/wiki/International_Bank_Account_Number#Validating_the_IBAN: Heuristics
    val withoutSpaces = string.withoutWhitespace()
    if (withoutSpaces.length < 10 || withoutSpaces.length > 34) return false
    return withoutSpaces[0].isLetter() && withoutSpaces[1].isLetter()
            && withoutSpaces[2].isDigit() && withoutSpaces[3].isDigit()
            && withoutSpaces.subSequence(4, withoutSpaces.length).all { it.isLetterOrDigit() }
}

fun describesBIC(string: String) = string.lowercase().withoutWhitespaceOrDashes() in listOf(
    "bic", "swiftaddress", "swiftcode"
)

fun isBIC(string: String): Boolean {
    // https://www.iban.com/glossary: Length is 8 or 11
    val withoutSpaces = string.withoutWhitespace()
    if (withoutSpaces.length !in listOf(8, 11)) return false
    return withoutSpaces.toCharArray().all { it.isLetterOrDigit() }
}

fun describesUrl(fieldName: String) = fieldName.lowercase().withoutWhitespaceOrDashes() in listOf(
    "url", "link", "homepage", // general
    "website", "webpage", "site", // english specific
    "webseite", "seite", "internetseite", // german specific
)

fun describesApp(fieldName: String) = fieldName.lowercase().withoutWhitespaceOrDashes() in listOf(
    "app", // general
    "application", // english specific
    "anwendung", // german specific
)

internal val EMAIL_REGEX: Regex by lazy { Regex("^[\\w-.%+-]+@([\\w-]+\\.)+[\\w-]{2,6}$") }

fun String.withoutWhitespaceOrDashes() = this.replace(Regex("[\\s-]+"), "")
fun String.withoutWhitespace() = this.replace(Regex("\\s+"), "")
