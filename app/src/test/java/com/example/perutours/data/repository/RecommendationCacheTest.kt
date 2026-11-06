package com.example.perutours.data.repository

import com.example.perutours.data.model.Recommendation
import com.example.perutours.data.model.RecommendationSource
import com.example.perutours.data.remote.DestinationDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HU10: la caché debe restaurar EXACTAMENTE el paquete que se mostró,
 * aunque el destino tenga varios paquetes publicados.
 */
class RecommendationCacheTest {

    private val packageEssential = DestinationDto(
        id = "pkg-esencial",
        destinationId = "cusco",
        title = "Cusco Esencial",
        location = "Cusco",
        duration = "3 días / 2 noches",
        price = "S/ 650",
        rating = "4.2",
        category = "Historia y Cultura",
        imageUrl = "https://img/esencial.jpg"
    )

    private val packagePremium = DestinationDto(
        id = "pkg-premium",
        destinationId = "cusco",
        title = "Cusco Premium",
        location = "Cusco",
        duration = "5 días / 4 noches",
        price = "S/ 1,400",
        rating = "4.9",
        category = "Historia y Cultura",
        imageUrl = "https://img/premium.jpg"
    )

    private val candidates = listOf(packageEssential, packagePremium)

    private val premiumRecommendation = Recommendation(
        destinationId = "cusco",
        title = "Cusco Premium",
        reason = "Encaja con tu tipo de viaje Cultura.",
        source = RecommendationSource.AI,
        destination = packagePremium
    )

    @Test
    fun savesTheExactPackageIdOfEachRecommendation() {
        val items = RecommendationRepository.buildCacheItems(
            listOf(premiumRecommendation)
        )

        assertEquals(1, items.size)
        assertEquals("cusco", items[0]["destinationId"])
        assertEquals("pkg-premium", items[0]["packageId"])
        assertEquals("Cusco Premium", items[0]["title"])
        assertEquals("Encaja con tu tipo de viaje Cultura.", items[0]["reason"])
        assertEquals("AI", items[0]["source"])
    }

    @Test
    fun restoresTheChosenPackageEvenWhenTheDestinationHasMany() {
        val items = RecommendationRepository.buildCacheItems(
            listOf(premiumRecommendation)
        )

        val restored = RecommendationRepository.restoreFromCacheItems(
            items, candidates
        )

        assertEquals(1, restored.size)
        val card = restored.single()

        // Conserva el paquete ELEGIDO: su precio, su imagen y su título...
        assertEquals("pkg-premium", card.destination?.id)
        assertEquals("S/ 1,400", card.destination?.price)
        assertEquals("https://img/premium.jpg", card.destination?.imageUrl)
        assertEquals("Cusco Premium", card.title)
        assertEquals("Encaja con tu tipo de viaje Cultura.", card.reason)
        // ...y no el otro paquete del mismo destino.
        assertEquals("S/ 650", packageEssential.price)
    }

    @Test
    fun dropsCachedItemsWhosePackageIsNoLongerPublished() {
        val items = RecommendationRepository.buildCacheItems(
            listOf(premiumRecommendation)
        )

        // El paquete elegido ya no está en el catálogo: mejor recomputar
        // que mezclar el título viejo con la imagen de otro paquete.
        val restored = RecommendationRepository.restoreFromCacheItems(
            items, listOf(packageEssential)
        )

        assertTrue(restored.isEmpty())
    }

    @Test
    fun legacyItemsWithoutPackageIdFallBackToDestinationId() {
        val legacyItems = listOf(
            mapOf(
                "destinationId" to "cusco",
                "title" to "Cusco Esencial",
                "reason" to "Motivo de una caché antigua",
                "source" to "LOCAL"
            )
        )

        val restored = RecommendationRepository.restoreFromCacheItems(
            legacyItems, candidates
        )

        assertEquals(1, restored.size)
        assertEquals("Cusco Esencial", restored.single().title)
        assertEquals("pkg-esencial", restored.single().destination?.id)
    }

    @Test
    fun collapsesDuplicatedDestinationsFromLegacyCaches() {
        // Caché anterior al fix: dos tarjetas apuntando al mismo destino.
        val legacyItems = listOf(
            mapOf(
                "destinationId" to "cusco",
                "title" to "Cusco Esencial",
                "reason" to "Motivo uno",
                "source" to "AI"
            ),
            mapOf(
                "destinationId" to "cusco",
                "title" to "Cusco Premium",
                "reason" to "Motivo dos",
                "source" to "AI"
            )
        )

        val restored = RecommendationRepository.restoreFromCacheItems(
            legacyItems, candidates
        )

        assertEquals(1, restored.size)
        assertEquals("Cusco Esencial", restored.single().title)
    }
}
