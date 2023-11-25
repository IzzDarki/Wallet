package com.izzdarki.wallet.logic.passwordgeneration

import java.security.SecureRandom
import java.util.LinkedList

object PasswordGeneration {
    const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    const val DIGITS = "0123456789"
    const val EASY_SPECIAL_CHARACTERS = "!?@#%&*+-=_/\\.,;:\"'{}()[]<>"
    const val DIFFICULT_SPECIAL_CHARACTERS = "$€¥§¢^`~|"
    const val CRAZY_CHARACTERS = "ℓ¡¢£¤¥¦§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ" // not for typing manually
    const val AMBIGUOUS_CHARACTERS = "Il|1o0O"
}

private fun <E> SecureRandom.permutation(permutationLength: Int, elements: Collection<E>): LinkedList<E> {
    fun <E> createPermutation(permutationLength: Int, elements: LinkedList<E>): LinkedList<E> {
        if (permutationLength == 0) return LinkedList()
        val element = elements.removeAt(this.nextInt(elements.size))
        val permutation = createPermutation(permutationLength - 1, elements)
        val index = this.nextInt(permutation.size + 1)
        permutation.add(index, element)
        return permutation
    }
    return createPermutation(permutationLength, LinkedList(elements))
}

fun generatePassword(
    characterSets: List<String>,
    length: Int,
    oneFromEach: Boolean,
    noAmbiguousCharacters: Boolean,
): String {
    val actualCharacterSets =
        if (noAmbiguousCharacters) characterSets.map { (it.toSet() - PasswordGeneration.AMBIGUOUS_CHARACTERS.toSet()).joinToString("") }
        else characterSets

    val randomGenerator = SecureRandom()
    val forcedIndices =
        if (oneFromEach) randomGenerator.permutation(actualCharacterSets.size, actualCharacterSets.indices.toList())
        else null
    return (0 until length)
        .map { index ->
            val characterSetIndex = forcedIndices?.getOrNull(index) ?: randomGenerator.nextInt(actualCharacterSets.size)
            val characterSet = actualCharacterSets[characterSetIndex]
            characterSet[randomGenerator.nextInt(characterSet.length)]
        }
        .joinToString("")
}
