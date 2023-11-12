package com.izzdarki.wallet.logic.autofill

import org.junit.Assert.*
import org.junit.Test

class FieldHeuristicsTest {

    @Test
    fun testEmailRegex() {
        assertTrue(emailHeuristic.isType!!("simple@email.com"))
        assertTrue(emailHeuristic.isType!!("simple@email.fr"))
        assertTrue(emailHeuristic.isType!!("more.complicated123@mail.fluff.ai"))
        assertTrue(emailHeuristic.isType!!("003crazy--23%asd_._adDrSs-E-maIL42@abs.003.fluff_flauch.ai-abc_aha32.9nine9"))
    }

    @Test
    fun testCreditCardNumber() {
        // https://www.validcreditcardnumber.com/
        assertTrue(creditCardNumberHeuristic.isType!!("371449635398431"))
        assertTrue(creditCardNumberHeuristic.isType!!("30569309025904"))
        assertTrue(creditCardNumberHeuristic.isType!!("6011111111111117"))
        assertTrue(creditCardNumberHeuristic.isType!!("3530111333300000"))
        assertTrue(creditCardNumberHeuristic.isType!!("5555555555554444"))
        assertTrue(creditCardNumberHeuristic.isType!!("4111111111111111"))

        assertFalse(creditCardNumberHeuristic.isType!!("37144963239843113"))
        assertFalse(creditCardNumberHeuristic.isType!!("371449632398431"))
        assertFalse(creditCardNumberHeuristic.isType!!("411111111111111"))
    }

    @Test
    fun testIBAN() {
        assertTrue(ibanHeuristic.isType!!("GB33BUKB20201555555555"))
        assertTrue(ibanHeuristic.isType!!("DE75512108001245126199"))
        assertTrue(ibanHeuristic.isType!!("FR7630006000011234567890189"))

        assertFalse(ibanHeuristic.isType!!("GBA33BUKB202015555542555"))
        assertFalse(ibanHeuristic.isType!!("DE755121080012451|261991"))
        assertFalse(ibanHeuristic.isType!!("FR763000600001123456789018948529458239502353452345"))
        assertFalse(ibanHeuristic.isType!!("FR6300120"))
    }

    @Test
    fun testBIC() {
        assertTrue(bicHeuristic.isType!!("BOFSGBS1ZF2"))
        assertTrue(bicHeuristic.isType!!("NORWNOK1"))
    }

}