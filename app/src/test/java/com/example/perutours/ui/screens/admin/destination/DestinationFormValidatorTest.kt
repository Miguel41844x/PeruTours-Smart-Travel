package com.example.perutours.ui.screens.admin.destination

import com.example.perutours.data.model.CatalogStatus
import com.example.perutours.data.model.TourDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DestinationFormValidatorTest {

    @Test
    fun `nombre muy corto es invalido`() {
        assertNotNull(DestinationFormValidator.validateName("Cu"))
        assertNotNull(DestinationFormValidator.validateName("   "))
    }

    @Test
    fun `nombre valido no tiene error`() {
        assertNull(DestinationFormValidator.validateName("Machu Picchu"))
    }

    @Test
    fun `nombre demasiado largo es invalido`() {
        val longName = "A".repeat(DestinationFormValidator.MAX_NAME_LENGTH + 1)
        assertNotNull(DestinationFormValidator.validateName(longName))
    }

    @Test
    fun `resumen vacio es valido pero el muy largo no`() {
        assertNull(DestinationFormValidator.validateSummary(""))
        val longSummary = "X".repeat(DestinationFormValidator.MAX_SUMMARY_LENGTH + 1)
        assertNotNull(DestinationFormValidator.validateSummary(longSummary))
    }

    @Test
    fun `imagen debe ser https`() {
        assertNull(DestinationFormValidator.validateImageUrl(""))
        assertNull(DestinationFormValidator.validateImageUrl("https://images.example.com/foto.jpg"))
        assertNotNull(DestinationFormValidator.validateImageUrl("http://insegura.com/foto.jpg"))
    }

    @Test
    fun `coordenadas vacias no son error porque son opcionales en borrador`() {
        assertNull(DestinationFormValidator.validateCoordinates("", ""))
    }

    @Test
    fun `coordenadas fuera de rango son invalidas`() {
        assertNotNull(DestinationFormValidator.validateCoordinates("120", "0"))
        assertNotNull(DestinationFormValidator.validateCoordinates("0", "200"))
        assertNotNull(DestinationFormValidator.validateCoordinates("12", ""))
    }

    @Test
    fun `coordenadas validas aceptan coma decimal`() {
        assertNull(DestinationFormValidator.validateCoordinates("-13,53195", "-71,96746"))
        assertEquals(-13.53195, DestinationFormValidator.parseCoordinate("-13,53195")!!, 0.0001)
    }

    @Test
    fun `publicar exige coordenadas`() {
        assertNotNull(DestinationFormValidator.validateForPublish("", ""))
        assertNull(DestinationFormValidator.validateForPublish("-13.53", "-71.96"))
    }

    @Test
    fun `parsea categorias separadas por coma y las deduplica`() {
        val categories = DestinationFormValidator.parseCategories(
            "Historia, Naturaleza , Historia, , Gastronomía"
        )
        assertEquals(listOf("Historia", "Naturaleza", "Gastronomía"), categories)
    }

    @Test
    fun `buildDestination conserva el id y los timestamps al editar`() {
        val existing = TourDestination(
            id = "dest_cusco",
            name = "Cusco",
            department = "Cusco",
            createdBy = "admin-1",
            createdAtMillis = 1_000L,
            status = CatalogStatus.BORRADOR.name
        )

        val updated = DestinationFormValidator.buildDestination(
            existing = existing,
            name = "Cusco Imperial",
            department = "Cusco",
            summary = "Nueva oferta",
            latitudeText = "-13.53",
            longitudeText = "-71.96",
            coverImageUrl = "https://example.com/img.jpg",
            categoriesText = "Historia",
            externalPlaceId = "place-123",
            status = CatalogStatus.PUBLICADO,
            // Sin autor informado: debe conservarse el creador original.
            createdBy = "",
            nowMillis = 2_000L
        )

        assertEquals("dest_cusco", updated.id)
        assertEquals("admin-1", updated.createdBy)
        assertEquals(1_000L, updated.createdAtMillis)
        assertEquals(2_000L, updated.updatedAtMillis)
        assertEquals(CatalogStatus.PUBLICADO.name, updated.status)
        assertEquals(-13.53, updated.latitude, 0.0001)
        assertEquals("place-123", updated.externalPlaceId)
    }
}
