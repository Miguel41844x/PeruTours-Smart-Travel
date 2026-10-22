package com.example.perutours.data.remote.geoapify

import com.google.gson.annotations.SerializedName

/**
 * Respuesta de la API de Geoapify (Geocoding y Places).
 *
 * Documentación: https://apidocs.geoapify.com/
 * Ambos servicios devuelven un FeatureCollection de GeoJSON, por lo que
 * comparten el mismo modelo base.
 */
data class GeoapifyFeatureCollection(
    @SerializedName("type")
    val type: String = "",

    @SerializedName("features")
    val features: List<GeoapifyFeature> = emptyList()
)

data class GeoapifyFeature(
    @SerializedName("properties")
    val properties: GeoapifyProperties = GeoapifyProperties(),

    @SerializedName("geometry")
    val geometry: GeoapifyGeometry? = null
)

data class GeoapifyProperties(
    @SerializedName("place_id")
    val placeId: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("formatted")
    val formatted: String = "",

    @SerializedName("city")
    val city: String? = null,

    @SerializedName("country")
    val country: String? = null,

    @SerializedName("categories")
    val categories: List<String> = emptyList(),

    @SerializedName("lon")
    val lon: Double? = null,

    @SerializedName("lat")
    val lat: Double? = null
)

data class GeoapifyGeometry(
    @SerializedName("type")
    val type: String = "",

    /** GeoJSON: [longitud, latitud]. */
    @SerializedName("coordinates")
    val coordinates: List<Double> = emptyList()
) {
    val longitude: Double? get() = coordinates.getOrNull(0)
    val latitude: Double? get() = coordinates.getOrNull(1)
}
