package com.example.perutours.data.remote.geoapify

import com.example.perutours.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API externa de Geoapify: geocodificación y puntos de interés.
 *
 * - Geocoding: valida el nombre de un destino y devuelve sus coordenadas.
 * - Places:    atractivos cercanos (museos, parques, restaurantes, etc.).
 *
 * La clave nunca se escribe en el código: se inyecta desde
 * local.properties (GEOAPIFY_API_KEY) mediante BuildConfig.
 */
interface GeoapifyApiService {

    @GET("v1/geocoding/search")
    suspend fun geocode(
        @Query("text") text: String,
        @Query("lang") lang: String = "es",
        @Query("filter") filter: String = "countrycode:pe",
        @Query("limit") limit: Int = 1,
        @Query("apiKey") apiKey: String = BuildConfig.GEOAPIFY_API_KEY
    ): GeoapifyFeatureCollection

    @GET("v2/places")
    suspend fun nearbyPlaces(
        /** Filtro circular: circle:latitud,longitud,radioEnMetros. */
        @Query("filter") filter: String,
        @Query("categories") categories: String,
        @Query("lang") lang: String = "es",
        @Query("limit") limit: Int = 20,
        @Query("sort") sort: String = "distance",
        @Query("apiKey") apiKey: String = BuildConfig.GEOAPIFY_API_KEY
    ): GeoapifyFeatureCollection
}
