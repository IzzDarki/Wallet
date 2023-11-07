package com.izzdarki.wallet.logic.autofill

import org.junit.Assert.*
import org.junit.Test

class FieldHeuristicsTest {

    @Test
    fun testEmailRegex() {
        assertTrue(isEmail("simple@email.com"))
        assertTrue(isEmail("simple@email.fr"))
        assertTrue(isEmail("more.complicated123@mail.fluff.ai"))
        assertTrue(isEmail("003crazy--23%asd_._adDrSs-E-maIL42@abs.003.fluff_flauch.ai-abc_aha32.9nine9"))
    }

    @Test
    fun testCreditCardNumber() {
        // https://www.validcreditcardnumber.com/
        assertTrue(isCreditCardNumber("371449635398431"))
        assertTrue(isCreditCardNumber("30569309025904"))
        assertTrue(isCreditCardNumber("6011111111111117"))
        assertTrue(isCreditCardNumber("3530111333300000"))
        assertTrue(isCreditCardNumber("5555555555554444"))
        assertTrue(isCreditCardNumber("4111111111111111"))

        assertFalse(isCreditCardNumber("37144963239843113"))
        assertFalse(isCreditCardNumber("371449632398431"))
        assertFalse(isCreditCardNumber("411111111111111"))
    }

    @Test
    fun testIBAN() {
        assertTrue(isIBAN("GB33BUKB20201555555555"))
        assertTrue(isIBAN("DE75512108001245126199"))
        assertTrue(isIBAN("FR7630006000011234567890189"))

        assertFalse(isIBAN("GBA33BUKB202015555542555"))
        assertFalse(isIBAN("DE755121080012451|261991"))
        assertFalse(isIBAN("FR763000600001123456789018948529458239502353452345"))
        assertFalse(isIBAN("FR6300120"))
    }

    @Test
    fun testBIC() {
        assertTrue(isBIC("BOFSGBS1ZF2"))
        assertTrue(isBIC("NORWNOK1"))
    }

}