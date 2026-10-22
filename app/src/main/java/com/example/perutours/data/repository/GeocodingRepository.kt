package com.example.perutours.data.repository

import com.example.perutours.data.remote.geoapify.GeoapifyApiService

/**
 * Geocodificación de destinos con la API externa de Geoapify.
 *
 * El administrador registra el nombre del destino y el servicio devuelve las
 * coordenadas reales (más el identificador externo del lugar). Así el
 * catálogo queda con coordenadas verificables y se pueden consultar después
 * los puntos de interés cercanos.
 */
class GeocodingRepository(
    private val api: GeoapifyApiService
) {

    data class GeocodingResult(
        val latitude: Double,
        val longitude: Double,
        val placeId: String,
        val label: String
    )

    /**
     * Busca el destino en Geoapify restringiendo la búsqueda a Perú.
     * Devuelve [Result.failure] si no hay coincidencias.
     */
    suspend fun geocodeDestination(
        name: String,
        department: String
    ): Result<GeocodingResult> = runCatching {
        val query = listOf(name, department, "Perú")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(", ")

        val response = api.geocode(text = query, lang = "es", limit = 1)
        val feature = response.features.firstOrNull()
            ?: throw IllegalStateException("Sin resultados")

        val properties = feature.properties
        val latitude = feature.geometry?.latitude ?: properties.lat
            ?: throw IllegalStateException("Sin coordenadas")
        val longitude = feature.geometry?.longitude ?: properties.lon
            ?: throw IllegalStateException("Sin coordenadas")

        GeocodingResult(
            latitude = latitude,
            longitude = longitude,
            placeId = properties.placeId,
            label = properties.formatted.ifBlank { query }
        )
    }
}
