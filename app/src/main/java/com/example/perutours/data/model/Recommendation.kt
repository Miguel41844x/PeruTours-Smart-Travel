package com.example.perutours.data.model

import com.example.perutours.data.remote.DestinationDto

/** Origen de una recomendación: inteligencia artificial o reglas locales. */
enum class RecommendationSource { AI, LOCAL }

/**
 * HU10: recomendación personalizada de un destino para el usuario.
 *
 * @param destinationId identificador del catálogo (navega al detalle).
 * @param reason frase personalizada que explica POR QUÉ se recomienda.
 * @param source indica si vino de Gemini (AI) o del fallback local (LOCAL).
 * @param destination datos completos del catálogo para pintar la tarjeta.
 * @param summary resumen turístico externo (Wikipedia), si está disponible.
 */
data class Recommendation(
    val destinationId: String,
    val title: String,
    val reason: String,
    val source: RecommendationSource = RecommendationSource.AI,
    val destination: DestinationDto? = null,
    val summary: String = ""
)

/**
 * Preferencias completas del usuario que alimentan al recomendador:
 * las estructuradas del perfil (HU10) + nombres de sus categorías de interés.
 */
data class TravelPreferences(
    val tripType: String = "",
    val budget: String = "",
    val duration: String = "",
    val interestNames: List<String> = emptyList()
)
