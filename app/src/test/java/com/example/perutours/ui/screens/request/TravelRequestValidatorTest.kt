package com.example.perutours.ui.screens.request

import com.example.perutours.data.model.TravelRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TravelRequestValidatorTest {
    private val now = 1_000_000L

    @Test
    fun validRequestHasNoErrors() {
        val result = TravelRequestValidator.validate(
            destination = "Cusco",
            originCity = "Lima",
            departureAtMillis = now + 10_000L,
            returnAtMillis = now + 20_000L,
            travelerCount = 2,
            nowMillis = now
        )

        assertTrue(result.isValid)
        assertNull(result.destinationError)
        assertNull(result.originCityError)
        assertNull(result.departureError)
        assertNull(result.returnError)
        assertNull(result.travelerCountError)
    }

    @Test
    fun blankRequiredFieldsReturnErrors() {
        val result = TravelRequestValidator.validate(
            destination = " ",
            originCity = "",
            departureAtMillis = 0L,
            returnAtMillis = 0L,
            travelerCount = 1,
            nowMillis = now
        )

        assertFalse(result.isValid)
        assertEquals("Ingresa un destino válido.", result.destinationError)
        assertEquals("Ingresa la ciudad de origen.", result.originCityError)
        assertEquals("Selecciona una fecha y hora de salida futura.", result.departureError)
    }

    @Test
    fun returnMustBeAfterDeparture() {
        val result = TravelRequestValidator.validate(
            destination = "Arequipa",
            originCity = "Lima",
            departureAtMillis = now + 20_000L,
            returnAtMillis = now + 10_000L,
            travelerCount = 1,
            nowMillis = now
        )

        assertFalse(result.isValid)
        assertEquals("El retorno debe ser posterior a la salida.", result.returnError)
    }

    @Test
    fun travelerCountAcceptsConfiguredLimits() {
        val minimum = validResultForTravelerCount(1)
        val maximum = validResultForTravelerCount(20)

        assertTrue(minimum.isValid)
        assertTrue(maximum.isValid)
    }

    @Test
    fun travelerCountRejectsValuesOutsideLimits() {
        assertFalse(validResultForTravelerCount(0).isValid)
        assertFalse(validResultForTravelerCount(21).isValid)
    }

    @Test
    fun newRequestUsesPendingQuoteStatus() {
        assertEquals(
            "Pendiente de cotizacion",
            TravelRequest().status
        )
    }

    private fun validResultForTravelerCount(count: Int): TravelRequestValidationResult {
        return TravelRequestValidator.validate(
            destination = "Cusco",
            originCity = "Lima",
            departureAtMillis = now + 10_000L,
            returnAtMillis = now + 20_000L,
            travelerCount = count,
            nowMillis = now
        )
    }
}
