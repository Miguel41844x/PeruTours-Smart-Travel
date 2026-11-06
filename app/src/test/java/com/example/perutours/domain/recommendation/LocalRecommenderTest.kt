package com.example.perutours.domain.recommendation

import com.example.perutours.data.model.RecommendationSource
import com.example.perutours.data.model.TravelPreferences
import com.example.perutours.data.remote.DestinationDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalRecommenderTest {

    private val adventure = DestinationDto(
        id = "1",
        destinationId = "1",
        title = "Trekking al Ausangate",
        location = "Cusco",
        duration = "5 días / 4 noches",
        price = "S/ 950",
        rating = "4.7",
        category = "Aventura y Trekking",
        imageUrl = ""
    )

    private val culture = DestinationDto(
        id = "2",
        destinationId = "2",
        title = "Machu Picchu",
        location = "Cusco",
        duration = "2 días / 1 noche",
        price = "S/ 1,299",
        rating = "4.9",
        category = "Historia y Cultura",
        imageUrl = ""
    )

    private val jungle = DestinationDto(
        id = "3",
        destinationId = "3",
        title = "Amazonía Iquitos",
        location = "Loreto",
        duration = "4 días / 3 noches",
        price = "S/ 480",
        rating = "4.8",
        category = "Naturaleza y Selva",
        imageUrl = ""
    )

    private val candidates = listOf(adventure, culture, jungle)

    @Test
    fun returnsEmptyListWhenThereAreNoCandidates() {
        val result = LocalRecommender.recommend(
            preferences = TravelPreferences(tripType = "Aventura"),
            candidates = emptyList()
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun ranksDestinationsMatchingTripTypeFirst() {
        val result = LocalRecommender.recommend(
            preferences = TravelPreferences(
                tripType = "Aventura",
                budget = "",
                duration = "",
                interestNames = emptyList()
            ),
            candidates = candidates
        )

        assertEquals("1", result.first().destinationId)
        assertEquals(RecommendationSource.LOCAL, result.first().source)
    }

    @Test
    fun prefersDestinationsMatchingInterestCategories() {
        val result = LocalRecommender.recommend(
            preferences = TravelPreferences(
                tripType = "",
                budget = "",
                duration = "",
                interestNames = listOf("Naturaleza y Selva")
            ),
            candidates = candidates
        )

        assertEquals("3", result.first().destinationId)
    }

    @Test
    fun matchesBudgetAgainstRealPackagePrice() {
        val result = LocalRecommender.recommend(
            preferences = TravelPreferences(
                tripType = "",
                budget = "Económico",
                duration = "",
                interestNames = emptyList()
            ),
            candidates = candidates
        )

        // La Amazonía (S/ 480) es la única dentro del tramo económico.
        assertEquals("3", result.first().destinationId)
    }

    @Test
    fun matchesDurationInDays() {
        val result = LocalRecommender.recommend(
            preferences = TravelPreferences(
                tripType = "",
                budget = "",
                duration = "Fin de semana",
                interestNames = emptyList()
            ),
            candidates = candidates
        )

        // Solo Machu Picchu (2 días) cabe en un fin de semana.
        assertEquals("2", result.first().destinationId)
    }

    @Test
    fun neverReturnsMoreThanTheRequestedLimit() {
        val result = LocalRecommender.recommend(
            preferences = TravelPreferences(),
            candidates = candidates,
            max = 2
        )

        assertEquals(2, result.size)
    }

    // ============ HU10 fix: destinos con varios paquetes ============

    @Test
    fun keepsOnlyOneCardPerDestinationWhenItHasManyPackages() {
        // Cusco publicado como 3 paquetes distintos + la Amazonía.
        val cuscoBasic = DestinationDto(
            id = "cusco-basico",
            destinationId = "cusco",
            title = "Cusco Esencial",
            location = "Cusco",
            duration = "3 días / 2 noches",
            price = "S/ 650",
            rating = "4.2",
            category = "Historia y Cultura",
            imageUrl = ""
        )
        val cuscoPremium = DestinationDto(
            id = "cusco-premium",
            destinationId = "cusco",
            title = "Cusco Premium",
            location = "Cusco",
            duration = "5 días / 4 noches",
            price = "S/ 1,400",
            rating = "4.9",
            category = "Historia y Cultura",
            imageUrl = ""
        )
        val cuscoMid = DestinationDto(
            id = "cusco-medio",
            destinationId = "cusco",
            title = "Cusco Clásico",
            location = "Cusco",
            duration = "4 días / 3 noches",
            price = "S/ 950",
            rating = "4.5",
            category = "Historia y Cultura",
            imageUrl = ""
        )

        val result = LocalRecommender.recommend(
            preferences = TravelPreferences(tripType = "Cultura"),
            candidates = listOf(cuscoBasic, cuscoPremium, cuscoMid, jungle)
        )

        // 4 paquetes → 2 destinos distintos (Cusco una sola vez + Amazonía).
        assertEquals(2, result.size)
        assertEquals(1, result.count { it.destinationId == "cusco" })
        assertEquals(1, result.count { it.destinationId == jungle.destinationId })
    }

    @Test
    fun keepsTheBestScoredPackageOfEachDuplicatedDestination() {
        val cuscoBasic = DestinationDto(
            id = "cusco-basico",
            destinationId = "cusco",
            title = "Cusco Esencial",
            location = "Cusco",
            duration = "3 días / 2 noches",
            price = "S/ 650",
            rating = "4.2",
            category = "Historia y Cultura",
            imageUrl = ""
        )
        val cuscoPremium = DestinationDto(
            id = "cusco-premium",
            destinationId = "cusco",
            title = "Cusco Premium",
            location = "Cusco",
            duration = "5 días / 4 noches",
            price = "S/ 1,400",
            rating = "4.9",
            category = "Historia y Cultura",
            imageUrl = ""
        )

        val result = LocalRecommender.recommend(
            preferences = TravelPreferences(tripType = "Cultura"),
            candidates = listOf(cuscoBasic, cuscoPremium, jungle)
        )

        // De los dos paquetes de Cusco se conserva el mejor puntuado.
        val cusco = result.first { it.destinationId == "cusco" }
        assertEquals("cusco-premium", cusco.destination?.id)
        assertEquals("S/ 1,400", cusco.destination?.price)
    }

    @Test
    fun everyRecommendationExplainsItself() {
        val result = LocalRecommender.recommend(
            preferences = TravelPreferences(tripType = "Cultura"),
            candidates = candidates
        )

        result.forEach { recommendation ->
            assertTrue(recommendation.reason.isNotBlank())
            assertFalse(recommendation.reason.endsWith(" "))
        }
    }

    // ===================== utilidades internas =====================

    @Test
    fun parsePriceUnderstandsSolesFormat() {
        assertEquals(1299.0, LocalRecommender.parsePrice("S/ 1,299")!!, 0.01)
        assertEquals(480.0, LocalRecommender.parsePrice("S/ 480")!!, 0.01)
        assertNull(LocalRecommender.parsePrice(""))
        assertNull(LocalRecommender.parsePrice("Consultar"))
    }

    @Test
    fun parseDaysUnderstandsDurationFormat() {
        assertEquals(4, LocalRecommender.parseDays("4 días / 3 noches"))
        assertEquals(2, LocalRecommender.parseDays("2 días / 1 noche"))
        assertNull(LocalRecommender.parseDays("Consultar"))
    }

    @Test
    fun durationRulesAcceptTheRightAmountOfDays() {
        assertTrue(LocalRecommender.fitsDuration(2, "Fin de semana"))
        assertTrue(LocalRecommender.fitsDuration(3, "Fin de semana"))
        assertFalse(LocalRecommender.fitsDuration(5, "Fin de semana"))

        assertTrue(LocalRecommender.fitsDuration(4, "3-5 días"))
        assertFalse(LocalRecommender.fitsDuration(7, "3-5 días"))

        assertTrue(LocalRecommender.fitsDuration(7, "1 semana"))
        assertFalse(LocalRecommender.fitsDuration(2, "1 semana"))

        assertTrue(LocalRecommender.fitsDuration(10, "2+ semanas"))
        assertFalse(LocalRecommender.fitsDuration(5, "2+ semanas"))
    }

    @Test
    fun normalizeRemovesAccentsAndCase() {
        assertEquals("gastronomia", LocalRecommender.normalize("Gastronomía"))
        assertEquals("historia y cultura", LocalRecommender.normalize("  Historia y Cultura "))
    }
}
