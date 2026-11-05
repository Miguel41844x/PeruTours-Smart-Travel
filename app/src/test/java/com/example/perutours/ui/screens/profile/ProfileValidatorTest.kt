package com.example.perutours.ui.screens.profile

import com.example.perutours.data.model.TravelProfileOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileValidatorTest {

    @Test
    fun validProfileHasNoErrors() {
        val result = ProfileValidator.validate(
            name = "Miguel Torres",
            phone = "987654321",
            dni = "",
            city = "Lima, Perú",
            selectedPreferences = setOf("Historia y Cultura"),
            tripType = "Cultura",
            budget = "Medio",
            duration = "1 semana"
        )

        assertTrue(result.isValid)
        assertNull(result.nameError)
        assertNull(result.phoneError)
        assertNull(result.cityError)
        assertNull(result.preferencesError)
        assertNull(result.tripTypeError)
        assertNull(result.budgetError)
        assertNull(result.durationError)
    }

    @Test
    fun blankRequiredFieldsReturnErrors() {
        val result = ProfileValidator.validate(
            name = "  ",
            phone = "",
            dni = "",
            city = " ",
            selectedPreferences = emptySet(),
            tripType = "",
            budget = "",
            duration = ""
        )

        assertFalse(result.isValid)
        assertEquals("El nombre completo es obligatorio.", result.nameError)
        assertEquals("El número de celular es obligatorio.", result.phoneError)
        assertEquals("La ciudad o país de origen es obligatorio.", result.cityError)
        assertEquals("Selecciona al menos una preferencia de viaje.", result.preferencesError)
        assertEquals("Selecciona tu tipo de viaje.", result.tripTypeError)
        assertEquals("Selecciona tu presupuesto de viaje.", result.budgetError)
        assertEquals("Selecciona la duración de tu viaje.", result.durationError)
    }

    @Test
    fun phoneMustHaveNineDigitsAndStartWithNine() {
        val result = ProfileValidator.validate(
            name = "Ana Ruiz",
            phone = "812345678",
            dni = "",
            city = "Cusco",
            selectedPreferences = setOf("Aventura y Trekking"),
            tripType = "Aventura",
            budget = "Económico",
            duration = "3-5 días"
        )

        assertFalse(result.isValid)
        assertEquals("El celular debe comenzar con 9.", result.phoneError)
    }

    @Test
    fun surroundingSpacesAreIgnored() {
        val result = ProfileValidator.validate(
            name = "  Ana Ruiz  ",
            phone = " 987654321 ",
            dni = "",
            city = "  Cusco  ",
            selectedPreferences = setOf("Gastronomía Peruana"),
            tripType = "  Gastronomía  ",
            budget = "  Premium  ",
            duration = "  Fin de semana  "
        )

        assertTrue(result.isValid)
    }

    // ===================== HU10: preferencias de viaje =====================

    @Test
    fun everyAllowedTripPreferenceCombinationIsValid() {
        for (tripType in TravelProfileOptions.tripTypes) {
            for (budget in TravelProfileOptions.budgets) {
                for (duration in TravelProfileOptions.durations) {
                    val result = ProfileValidator.validate(
                        name = "Miguel Torres",
                        phone = "987654321",
                        dni = "",
                        city = "Lima, Perú",
                        selectedPreferences = setOf("Historia y Cultura"),
                        tripType = tripType,
                        budget = budget,
                        duration = duration
                    )

                    assertTrue(
                        "Combinación inválida: $tripType / $budget / $duration",
                        result.isValid
                    )
                }
            }
        }
    }

    @Test
    fun unknownTripTypeReturnsSpecificError() {
        val result = ProfileValidator.validate(
            name = "Miguel Torres",
            phone = "987654321",
            dni = "",
            city = "Lima, Perú",
            selectedPreferences = setOf("Historia y Cultura"),
            tripType = "Extreme",
            budget = "Medio",
            duration = "1 semana"
        )

        assertFalse(result.isValid)
        assertEquals("Selecciona tu tipo de viaje.", result.tripTypeError)
        assertNull(result.budgetError)
        assertNull(result.durationError)
    }

    @Test
    fun unknownBudgetReturnsSpecificError() {
        val result = ProfileValidator.validate(
            name = "Miguel Torres",
            phone = "987654321",
            dni = "",
            city = "Lima, Perú",
            selectedPreferences = setOf("Historia y Cultura"),
            tripType = "Cultura",
            budget = "Muy caro",
            duration = "1 semana"
        )

        assertFalse(result.isValid)
        assertEquals("Selecciona tu presupuesto de viaje.", result.budgetError)
        assertNull(result.tripTypeError)
        assertNull(result.durationError)
    }

    @Test
    fun unknownDurationReturnsSpecificError() {
        val result = ProfileValidator.validate(
            name = "Miguel Torres",
            phone = "987654321",
            dni = "",
            city = "Lima, Perú",
            selectedPreferences = setOf("Historia y Cultura"),
            tripType = "Cultura",
            budget = "Medio",
            duration = "Un mes"
        )

        assertFalse(result.isValid)
        assertEquals("Selecciona la duración de tu viaje.", result.durationError)
        assertNull(result.tripTypeError)
        assertNull(result.budgetError)
    }

    @Test
    fun blankTripPreferencesReturnAllThreeErrors() {
        val result = ProfileValidator.validate(
            name = "Miguel Torres",
            phone = "987654321",
            dni = "",
            city = "Lima, Perú",
            selectedPreferences = setOf("Historia y Cultura"),
            tripType = "",
            budget = "",
            duration = ""
        )

        assertFalse(result.isValid)
        assertEquals("Selecciona tu tipo de viaje.", result.tripTypeError)
        assertEquals("Selecciona tu presupuesto de viaje.", result.budgetError)
        assertEquals("Selecciona la duración de tu viaje.", result.durationError)
    }
}
