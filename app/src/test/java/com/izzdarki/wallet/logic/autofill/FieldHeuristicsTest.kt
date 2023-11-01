package com.izzdarki.wallet.logic.autofill

import org.junit.Assert.*
import org.junit.Test

class FieldHeuristicsTest {

    @Test
    fun testIfTopLevelDomainsSorted() {
        val sorted = TOP_LEVEL_DOMAINS_SORTED.sorted()
        assertEquals(sorted, TOP_LEVEL_DOMAINS_SORTED)
    }

    @Test
    fun testEmailRegex() {
        assertTrue(isEmail("simple@email.com"))
        assertTrue(isEmail("simple@email.fr"))
        assertTrue(isEmail("more.complicated123@mail.fluff.ai"))
        assertTrue(isEmail("003crazy--23%asd_._adDrSs-E-maIL42@abs.003.fluff_flauch.ai-abc_aha32.9nine9"))
    }

}