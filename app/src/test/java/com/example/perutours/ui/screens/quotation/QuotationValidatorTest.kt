package com.example.perutours.ui.screens.quotation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuotationValidatorTest {

    @Test
    fun validityDaysAcceptConfiguredBoundaries() {
        assertNull(QuotationValidator.validateValidityDays(1))
        assertNull(QuotationValidator.validateValidityDays(30))
    }

    @Test
    fun validityDaysRejectValuesOutsideConfiguredRange() {
        assertEquals(
            "La vigencia debe encontrarse entre 1 y 30 días.",
            QuotationValidator.validateValidityDays(0)
        )
        assertEquals(
            "La vigencia debe encontrarse entre 1 y 30 días.",
            QuotationValidator.validateValidityDays(31)
        )
    }

    @Test
    fun conditionsAcceptConfiguredLengthLimit() {
        assertNull(
            QuotationValidator.validateConditions(
                "a".repeat(QuotationValidator.MAX_CONDITIONS_LENGTH)
            )
        )
    }

    @Test
    fun conditionsRejectTextOverConfiguredLengthLimit() {
        assertEquals(
            "Las condiciones no pueden superar 2000 caracteres.",
            QuotationValidator.validateConditions(
                "a".repeat(QuotationValidator.MAX_CONDITIONS_LENGTH + 1)
            )
        )
    }
}
