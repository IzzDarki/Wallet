package com.izzdarki.wallet.logic

import kotlin.random.Random

fun generateNewId(usedIds: Collection<Int>): Int {
    return generateNewId { id -> id !in usedIds }
}

fun generateNewId(isIdValid: (Int) -> Boolean): Int {
    var newId: Int
    do {
        newId = Random.nextInt()
    } while (!isIdValid(newId))
    return newId
}