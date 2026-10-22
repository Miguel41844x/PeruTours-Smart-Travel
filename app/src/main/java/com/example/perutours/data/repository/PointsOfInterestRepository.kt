package com.example.perutours.data.repository

import com.example.perutours.data.local.PlaceDao
import com.example.perutours.data.local.PlaceEntity
import com.example.perutours.data.remote.geoapify.GeoapifyApiService
import com.example.perutours.data.remote.geoapify.GeoapifyFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Puntos de interés cercanos a un destino usando la API externa de
 * Geoapify (Places), con caché local en Room.
 *
 * El cliente consulta los atractivos turísticos, museos, parques y
 * restaurantes cerca de las coordenadas del destino. Room guarda el
 * resultado para no repetir llamadas y soportar modo offline.
 */
class PointsOfInterestRepository(
    private val api: GeoapifyApiService,
    private val placeDao: PlaceDao,
    private val cacheTtlMillis: Long = 60 * 60 * 1000L, // 1 hora
    private val radiusMeters: Int = 5_000,
    private val maxResults: Int = 20
) {

    data class NearbyPlace(
        val id: String,
        val name: String,
        val categoryLabel: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val distanceMeters: Double
    )

    /**
     * Devuelve los lugares cercanos al destino. Usa la caché local si está
     * vigente; si no, consulta Geoapify y repuebla Room.
     */
    suspend fun getNearbyPlaces(
        destinationId: String,
        latitude: Double,
        longitude: Double,
        forceRefresh: Boolean = false
    ): Result<List<NearbyPlace>> = withContext(Dispatchers.IO) {
        runCatching {
            if (destinationId.isBlank() || (latitude == 0.0 && longitude == 0.0)) {
                return@runCatching emptyList<NearbyPlace>()
            }

            if (!forceRefresh && isCacheFresh(destinationId)) {
                val cached = placeDao.getPlacesForDestination(destinationId)
                if (cached.isNotEmpty()) {
                    return@runCatching cached.map { it.toDomain() }
                }
            }

            val response = api.nearbyPlaces(
                filter = "circle:$longitude,$latitude,$radiusMeters",
                categories = TOURISM_CATEGORIES,
                limit = maxResults
            )

            val entities = response.features
                .mapNotNull { feature -> feature.toEntity(destinationId, latitude, longitude) }
                .sortedBy { it.distanceMeters }

            if (entities.isNotEmpty()) {
                placeDao.syncPlacesForDestination(destinationId, entities)
            }

            entities.map { it.toDomain() }
        }
    }

    private suspend fun isCacheFresh(destinationId: String): Boolean {
        val lastFetched = placeDao.getLastFetchedTimestamp(destinationId) ?: return false
        return System.currentTimeMillis() - lastFetched <= cacheTtlMillis
    }

    private fun GeoapifyFeature.toEntity(
        destinationId: String,
        originLatitude: Double,
        originLongitude: Double
    ): PlaceEntity? {
        val latitude = geometry?.latitude ?: properties.lat ?: return null
        val longitude = geometry?.longitude ?: properties.lon ?: return null
        val name = properties.name.ifBlank { properties.formatted }.ifBlank { return null }
        val id = properties.placeId.ifBlank { "${name}_$latitude,$longitude" }

        return PlaceEntity(
            id = id,
            destinationId = destinationId,
            name = name,
            categories = properties.categories.joinToString(", "),
            address = properties.formatted,
            latitude = latitude,
            longitude = longitude,
            distanceMeters = distanceInMeters(originLatitude, originLongitude, latitude, longitude),
            fetchedAtMillis = System.currentTimeMillis()
        )
    }

    private fun PlaceEntity.toDomain(): NearbyPlace = NearbyPlace(
        id = id,
        name = name,
        categoryLabel = categories.split(", ")
            .map { it.substringAfterLast('.') }
            .filter { it.isNotBlank() }
            .joinToString(" · "),
        address = address,
        latitude = latitude,
        longitude = longitude,
        distanceMeters = distanceMeters
    )

    companion object {
        /**
         * Categorías de Geoapify para turismo: atracciones, museos,
         * parques, esparcimiento y gastronomía.
         */
        const val TOURISM_CATEGORIES =
            "tourism.sights,tourism.attraction,leisure.park,entertainment,catering.restaurant"

        /** Distancia en metros entre dos puntos (fórmula de Haversine). */
        fun distanceInMeters(
            originLatitude: Double,
            originLongitude: Double,
            destinationLatitude: Double,
            destinationLongitude: Double
        ): Double {
            val earthRadius = 6_371_000.0
            val deltaLatitude = Math.toRadians(destinationLatitude - originLatitude)
            val deltaLongitude = Math.toRadians(destinationLongitude - originLongitude)
            val originLat = Math.toRadians(originLatitude)
            val destinationLat = Math.toRadians(destinationLatitude)

            val a = sin(deltaLatitude / 2) * sin(deltaLatitude / 2) +
                cos(originLat) * cos(destinationLat) *
                sin(deltaLongitude / 2) * sin(deltaLongitude / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))

            return earthRadius * c
        }
    }
}
