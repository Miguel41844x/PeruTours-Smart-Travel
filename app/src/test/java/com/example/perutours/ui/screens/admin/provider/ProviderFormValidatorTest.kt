package com.example.perutours.ui.screens.admin.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderFormValidatorTest {

    @Test
    fun `nombre del proveedor debe tener al menos 2 caracteres`() {
        assertNotNull(ProviderFormValidator.validateName("A"))
        assertNull(ProviderFormValidator.validateName("Andes Travel"))
    }

    @Test
    fun `correo con formato invalido genera error`() {
        assertNotNull(ProviderFormValidator.validateEmail(""))
        assertNotNull(ProviderFormValidator.validateEmail("contacto@"))
        assertNotNull(ProviderFormValidator.validateEmail("contactoandestravel"))
        assertNull(ProviderFormValidator.validateEmail("contacto@andestravel.pe"))
    }

    @Test
    fun `telefono admite prefijo internacional y espacios`() {
        assertNull(ProviderFormValidator.validatePhone(""))
        assertNull(ProviderFormValidator.validatePhone("+51 987 654 321"))
        assertNull(ProviderFormValidator.validatePhone("987654321"))
        assertNotNull(ProviderFormValidator.validatePhone("123"))
        assertNotNull(ProviderFormValidator.validatePhone("123456789012345"))
    }

    @Test
    fun `ciudad es obligatoria`() {
        assertNotNull(ProviderFormValidator.validateCity(""))
        assertNotNull(ProviderFormValidator.validateCity("C"))
        assertNull(ProviderFormValidator.validateCity("Cusco"))
    }

    @Test
    fun `parsea servicios y descarta vacios`() {
        val services = ProviderFormValidator.parseServices("Hoteles, Transporte, , Guías")
        assertEquals(listOf("Hoteles", "Transporte", "Guías"), services)
    }

    @Test
    fun `buildProvider conserva id y createdAt al editar`() {
        val existing = com.example.perutours.data.model.TourProvider(
            id = "prov_1",
            name = "Andes Travel",
            createdAtMillis = 500L
        )

        val updated = ProviderFormValidator.buildProvider(
            existing = existing,
            name = "Andes Travel SAC",
            contactName = "María Rojas",
            phone = "987654321",
            email = "maria@andestavel.pe",
            city = "Cusco",
            servicesText = "Hoteles",
            active = false,
            nowMillis = 900L
        )

        assertEquals("prov_1", updated.id)
        assertEquals(500L, updated.createdAtMillis)
        assertEquals(900L, updated.updatedAtMillis)
        assertEquals(false, updated.active)
        assertEquals(listOf("Hoteles"), updated.serviceNames)
    }
}
