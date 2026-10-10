package com.example.perutours.ui.screens.profile

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
            selectedPreferences = setOf("Historia y Cultura")
        )

        assertTrue(result.isValid)
        assertNull(result.nameError)
        assertNull(result.phoneError)
        assertNull(result.cityError)
        assertNull(result.preferencesError)
    }

    @Test
    fun blankRequiredFieldsReturnErrors() {
        val result = ProfileValidator.validate(
            name = "  ",
            phone = "",
            dni = "",
            city = " ",
            selectedPreferences = emptySet()
        )

        assertFalse(result.isValid)
        assertEquals("El nombre completo es obligatorio.", result.nameError)
        assertEquals("Ingresa un celular válido de 9 dígitos que empiece con 9.", result.phoneError)
        assertEquals("La ciudad o país de origen es obligatorio.", result.cityError)
        assertEquals("Selecciona al menos una preferencia de viaje.", result.preferencesError)
    }

    @Test
    fun phoneMustHaveNineDigitsAndStartWithNine() {
        val result = ProfileValidator.validate(
            name = "Ana Ruiz",
            phone = "812345678",
            dni = "",
            city = "Cusco",
            selectedPreferences = setOf("Aventura y Trekking")
        )

        assertFalse(result.isValid)
        assertEquals("Ingresa un celular válido de 9 dígitos que empiece con 9.", result.phoneError)
    }

    @Test
    fun surroundingSpacesAreIgnored() {
        val result = ProfileValidator.validate(
            name = "  Ana Ruiz  ",
            phone = " 987654321 ",
            dni = "",
            city = "  Cusco  ",
            selectedPreferences = setOf("Gastronomía Peruana")
        )

        assertTrue(result.isValid)
    }
}
