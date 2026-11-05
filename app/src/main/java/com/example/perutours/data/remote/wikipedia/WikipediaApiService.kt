package com.example.perutours.data.remote.wikipedia

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

/**
 * API externa de información turística (HU10): resúmenes de Wikipedia
 * en español para enriquecer cada recomendación con contexto real.
 *
 * Es gratuita, no requiere clave y su política de uso exige un
 * User-Agent identificable (por eso se envía el de PeruTours).
 */
interface WikipediaApiService {

    @Headers(
        "User-Agent: PeruTours/1.0 (app de recomendaciones turísticas; https://github.com/Miguel41844x/PeruTours-Smart-Travel)",
        "Accept: application/json"
    )
    @GET("page/summary/{title}")
    suspend fun summary(@Path("title") title: String): WikipediaSummaryDto
}
