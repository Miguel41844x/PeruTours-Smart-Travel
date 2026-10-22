package com.example.perutours.ui.screens.admin.packages

import com.example.perutours.data.model.CatalogStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PackageFormValidatorTest {

    @Test
    fun `titulo debe tener al menos 3 caracteres`() {
        assertNotNull(PackageFormValidator.validateTitle("M "))
        assertNotNull(PackageFormValidator.validateTitle("   "))
        assertNull(PackageFormValidator.validateTitle("Machu Picchu Mágico"))
    }

    @Test
    fun `destino es obligatorio`() {
        assertNotNull(PackageFormValidator.validateDestination(""))
        assertNull(PackageFormValidator.validateDestination("dest_cusco"))
    }

    @Test
    fun `parsea precios con formato peruano`() {
        assertEquals(1299.0, PackageFormValidator.parsePrice("S/ 1,299")!!, 0.01)
        assertEquals(480.0, PackageFormValidator.parsePrice("480")!!, 0.01)
        assertEquals(650.0, PackageFormValidator.parsePrice("S/ 650")!!, 0.01)
    }

    @Test
    fun `precio invalido o negativo genera error`() {
        assertNotNull(PackageFormValidator.validatePrice(""))
        assertNotNull(PackageFormValidator.validatePrice("gratis"))
        assertNotNull(PackageFormValidator.validatePrice("-100"))
        assertNull(PackageFormValidator.validatePrice("S/ 1,299"))
    }

    @Test
    fun `construye la etiqueta de duración`() {
        assertEquals("4 días / 3 noches", PackageFormValidator.buildDurationText(4, 3))
        assertEquals("1 día / 0 noches", PackageFormValidator.buildDurationText(1, 0))
        assertEquals("2 días / 2 noches", PackageFormValidator.buildDurationText(2, 99))
    }

    @Test
    fun `el paquete nuevo nace en borrador con timestamps actuales`() {
        val tourPackage = PackageFormValidator.buildPackage(
            existing = null,
            destinationId = "dest_cusco",
            title = "Machu Picchu Mágico",
            summary = "Explora la ciudadela inca.",
            durationText = "4 días / 3 noches",
            durationDays = 4,
            priceText = "S/ 1,299",
            currency = "PEN",
            ratingScore = 4.9,
            category = "Historia y Cultura",
            coverImageUrl = "https://example.com/img.jpg",
            conditions = "No reembolsable",
            includedServiceIds = listOf("srv_1", "srv_1", "srv_2"),
            status = CatalogStatus.BORRADOR,
            createdBy = "admin-1",
            nowMillis = 1_000L
        )

        assertEquals(CatalogStatus.BORRADOR.name, tourPackage.status)
        assertEquals("admin-1", tourPackage.createdBy)
        assertEquals(1_000L, tourPackage.createdAtMillis)
        assertEquals(1299.0, tourPackage.priceAmount, 0.01)
        // La lista de servicios se deduplica al persistir.
        assertEquals(listOf("srv_1", "srv_2"), tourPackage.includedServiceIds)
        assertEquals("S/ 1 299", tourPackage.priceLabel())
    }
}
