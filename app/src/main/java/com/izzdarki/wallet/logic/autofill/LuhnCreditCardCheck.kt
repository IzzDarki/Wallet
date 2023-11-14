package com.izzdarki.wallet.logic.autofill

/**
 * Simple check to check if a credit card number is valid according to the Luhn algorithm.
 * Meant to find accidental typos, not to prevent attacks.
 */
internal fun passesLuhnCheck(numberString: String): Boolean {
    val parity = numberString.length % 2
    val checksum = numberString.toCharArray()
        .map { character -> character.digitToIntOrNull(10) ?: return false}
        .foldIndexed(initial = 0) {
            index, acc, digit ->
            val doubled = if (index % 2 == parity) digit * 2 else digit
            acc + if (doubled > 9) doubled - 9 else doubled
        }
    return checksum.mod(10) == 0
}