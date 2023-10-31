package com.izzdarki.wallet.logic

import kotlin.random.Random

fun generateNewId(usedIds: Collection<Long>): Long {
    return generateNewId { id -> id !in usedIds }
}

fun generateNewId(isIdValid: (Long) -> Boolean): Long {
    var newId: Long
    do {
        newId = Random.nextLong()
    } while (!isIdValid(newId))
    return newId
}