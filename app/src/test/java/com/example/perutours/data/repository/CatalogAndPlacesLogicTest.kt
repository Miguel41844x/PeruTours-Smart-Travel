package com.example.perutours.data.repository

import com.example.perutours.data.model.CatalogStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de lógica pura del catálogo: distancia de los puntos de interés
 * (Geoapify) y transiciones de estado de destinos y paquetes.
 */
class CatalogAndPlacesLogicTest {

    @Test
    fun `distancia entre Cusco y Lima es coherente`() {
        // Cusco (-13.53, -71.97) y Lima (-12.05, -77.04): ~560 km
        val distance = PointsOfInterestRepository.distanceInMeters(
            -13.53195, -71.96746,
            -12.0464, -77.0428
        )
        assertEquals(560_000.0, distance, 30_000.0)
    }

    @Test
    fun `distancia al mismo punto es cero`() {
        val distance = PointsOfInterestRepository.distanceInMeters(
            -13.53195, -71.96746,
            -13.53195, -71.96746
        )
        assertEquals(0.0, distance, 0.5)
    }

    @Test
    fun `estados del catálogo comparan sin distinguir mayúsculas`() {
        assertEquals(CatalogStatus.PUBLICADO, CatalogStatus.from("publicado"))
        assertEquals(CatalogStatus.BORRADOR, CatalogStatus.from(null))
        assertEquals(CatalogStatus.ARCHIVADO, CatalogStatus.from("ARCHIVADO"))
        assertEquals(CatalogStatus.BORRADOR, CatalogStatus.from("valor_inesperado"))
    }

    @Test
    fun `solo un destino publicado es visible para el cliente`() {
        val published = CatalogStatus.from("PUBLICADO")
        val draft = CatalogStatus.from("BORRADOR")
        val archived = CatalogStatus.from("ARCHIVADO")

        assertTrue(published == CatalogStatus.PUBLICADO)
        assertFalse(draft == CatalogStatus.PUBLICADO)
        assertFalse(archived == CatalogStatus.PUBLICADO)
    }
}
