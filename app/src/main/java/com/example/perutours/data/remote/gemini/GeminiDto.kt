package com.example.perutours.data.remote.gemini

import com.google.gson.annotations.SerializedName

/**
 * Modelos de transporte de la API REST de Gemini
 * (generativelanguage.googleapis.com → generateContent).
 *
 * Solo describen el envelope HTTP; los modelos de dominio de las
 * recomendaciones viven en data/model/Recommendation.kt.
 */

data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    val role: String = "user",
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiGenerationConfig(
    val temperature: Double? = null,
    val maxOutputTokens: Int? = null,
    /** "application/json" fuerza a Gemini a responder JSON estricto. */
    @SerializedName("responseMimeType")
    val responseMimeType: String? = null
)

data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate>? = null
) {
    /** Texto completo de la respuesta (primer candidate). */
    fun text(): String = candidates?.firstOrNull()?.text().orEmpty()
}

data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null
) {
    /** Texto completo de la respuesta (primer part del primer candidate). */
    fun text(): String =
        content?.parts?.firstOrNull()?.text.orEmpty()
}
