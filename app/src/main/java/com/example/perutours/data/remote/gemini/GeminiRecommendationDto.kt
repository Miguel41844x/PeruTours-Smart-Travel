package com.example.perutours.data.remote.gemini

import com.google.gson.annotations.SerializedName

/**
 * JSON estricto que Gemini debe devolver para la HU10.
 *
 * El prompt (RecommendationPromptBuilder) solicita exactamente este
 * esquema y se envía con responseMimeType=application/json para minimizar
 * la probabilidad de respuesta en texto libre.
 *
 * Ejemplo:
 * {"recommendations":[
 *   {"destinationId":"abc123","title":"Machu Picchu","reason":"Porque buscas aventura..."}
 * ]}
 */
data class GeminiRecommendationResponse(
    val recommendations: List<GeminiRecommendedDestination>? = null
)

data class GeminiRecommendedDestination(
    @SerializedName("destinationId")
    val destinationId: String = "",
    val title: String = "",
    val reason: String = ""
)
