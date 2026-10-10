package com.example.perutours.ui.screens.quotation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuotationResponseValidatorTest {

    @Test
    fun blankObservationReturnsError() {
        assertEquals(
            "Escribe una observación antes de enviarla.",
            QuotationResponseValidator.validateObservation("   ")
        )
    }

    @Test
    fun shortObservationReturnsError() {
        assertEquals(
            "La observación debe tener al menos 10 caracteres.",
            QuotationResponseValidator.validateObservation("Muy corto")
        )
    }

    @Test
    fun observationAtMinimumLengthIsValid() {
        assertNull(QuotationResponseValidator.validateObservation("1234567890"))
    }

    @Test
    fun observationOverMaximumLengthReturnsError() {
        val message = "a".repeat(QuotationResponseValidator.MAX_OBSERVATION_LENGTH + 1)

        assertEquals(
            "La observación no puede superar los 500 caracteres.",
            QuotationResponseValidator.validateObservation(message)
        )
    }
}
