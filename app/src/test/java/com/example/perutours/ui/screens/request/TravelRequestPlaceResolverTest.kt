package com.example.perutours.ui.screens.request

import com.example.perutours.data.location.DetectedLocation
import com.example.perutours.data.location.LocationDataSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TravelRequestPlaceResolverTest {

    @Test
    fun manualPlacesAreResolvedAndNormalized() = runBlocking {
        val destination = DetectedLocation("Cusco, Perú", -13.5319, -71.9675)
        val origin = DetectedLocation("Lima, Perú", -12.0464, -77.0428)
        val resolver = TravelRequestPlaceResolver(
            FakeLocationDataSource(
                places = mapOf("cusco" to destination, "lima" to origin)
            )
        )

        val result = resolver.resolve("cusco", "lima", null, null)

        val success = result as PlaceResolutionResult.Success
        assertEquals(destination, success.destination)
        assertEquals(origin, success.origin)
    }

    @Test
    fun detectedOriginKeepsGpsCoordinatesWithoutGeocodingItAgain() = runBlocking {
        val destination = DetectedLocation("Arequipa, Perú", -16.409, -71.5375)
        val dataSource = FakeLocationDataSource(mapOf("arequipa" to destination))
        val resolver = TravelRequestPlaceResolver(dataSource)

        val result = resolver.resolve(
            destinationQuery = "arequipa",
            originCity = "Lima",
            originLatitude = -12.0464,
            originLongitude = -77.0428
        )

        val success = result as PlaceResolutionResult.Success
        assertEquals("Lima", success.origin.city)
        assertEquals(-12.0464, success.origin.latitude, 0.0)
        assertEquals(listOf("arequipa"), dataSource.queries)
    }

    @Test
    fun unknownDestinationStopsBeforeResolvingOrigin() = runBlocking {
        val dataSource = FakeLocationDataSource(emptyMap())
        val resolver = TravelRequestPlaceResolver(dataSource)

        val result = resolver.resolve("inventado", "lima", null, null)

        assertSame(PlaceResolutionResult.InvalidDestination, result)
        assertEquals(listOf("inventado"), dataSource.queries)
    }

    @Test
    fun unknownManualOriginReturnsSpecificResult() = runBlocking {
        val destination = DetectedLocation("Cusco, Perú", -13.5319, -71.9675)
        val resolver = TravelRequestPlaceResolver(
            FakeLocationDataSource(mapOf("cusco" to destination))
        )

        val result = resolver.resolve("cusco", "inventado", null, null)

        assertSame(PlaceResolutionResult.InvalidOrigin, result)
    }

    private class FakeLocationDataSource(
        private val places: Map<String, DetectedLocation>
    ) : LocationDataSource {
        val queries = mutableListOf<String>()

        override suspend fun getCurrentLocation(): DetectedLocation =
            error("No se usa en estas pruebas")

        override suspend fun findPlace(query: String): DetectedLocation? {
            queries += query
            return places[query]
        }
    }
}
