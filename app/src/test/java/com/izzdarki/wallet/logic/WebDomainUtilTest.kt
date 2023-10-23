package com.izzdarki.wallet.logic

import com.izzdarki.wallet.logic.autofill.TOP_LEVEL_DOMAINS_SORTED
import org.junit.Assert.*
import org.junit.Test

class WebDomainUtilTest {

    @Test
    fun checkIfTopLevelDomainsSorted() {
        val sorted = TOP_LEVEL_DOMAINS_SORTED.sorted()
        assertEquals(sorted, TOP_LEVEL_DOMAINS_SORTED)
    }

}