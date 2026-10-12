package com.example.perutours.ui.screens.quotation

import com.example.perutours.data.model.Quotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

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

    @Test
    fun activeQuotationCanBeAcceptedDuringItsLastValidDay() {
        val quotation = Quotation(validityDate = "2026-10-11")

        assertNull(
            QuotationResponseValidator.validateAcceptance(
                quotation = quotation,
                nowMillis = localMillis(2026, Calendar.OCTOBER, 11, 23, 59)
            )
        )
    }

    @Test
    fun expiredQuotationCannotBeAccepted() {
        val quotation = Quotation(validityDate = "2026-10-11")

        assertEquals(
            "La cotización venció. Solicita una propuesta actualizada.",
            QuotationResponseValidator.validateAcceptance(
                quotation = quotation,
                nowMillis = localMillis(2026, Calendar.OCTOBER, 12, 0, 0)
            )
        )
    }

    @Test
    fun serverDeadlineTakesPriorityOverDisplayDate() {
        val now = localMillis(2026, Calendar.OCTOBER, 11, 12, 0)
        val quotation = Quotation(
            validityDate = "2026-10-20",
            validUntilMillis = now - 1L
        )

        assertEquals(
            "La cotización venció. Solicita una propuesta actualizada.",
            QuotationResponseValidator.validateAcceptance(quotation, now)
        )
    }

    @Test
    fun invalidValidityDateCannotBeAccepted() {
        val quotation = Quotation(validityDate = "11/10/2026")

        assertEquals(
            "La cotización no tiene una fecha de vigencia válida.",
            QuotationResponseValidator.validateAcceptance(quotation)
        )
    }

    @Test
    fun previouslyAnsweredQuotationCannotBeAcceptedAgain() {
        val quotation = Quotation(
            status = Quotation.STATUS_ACCEPTED,
            validityDate = "2026-10-11"
        )

        assertEquals(
            "La cotización ya fue respondida.",
            QuotationResponseValidator.validateAcceptance(quotation)
        )
    }

    private fun localMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): Long = Calendar.getInstance().apply {
        clear()
        set(year, month, day, hour, minute)
    }.timeInMillis
}
