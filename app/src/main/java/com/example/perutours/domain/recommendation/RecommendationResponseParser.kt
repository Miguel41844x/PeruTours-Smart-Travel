package com.example.perutours.domain.recommendation

import com.example.perutours.data.remote.gemini.GeminiRecommendationResponse
import com.example.perutours.data.remote.gemini.GeminiRecommendedDestination
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

/**
 * HU10: extrae y valida el JSON de recomendaciones devuelto por Gemini.
 *
 * Aunque el prompt pide JSON estricto, un LLM a veces envuelve la
 * respuesta en bloques ```json o agrega texto alrededor; este parser
 * tolera ambos casos y siempre devuelve una lista (vacía si hay fallo).
 */
object RecommendationResponseParser {

    private val gson = Gson()

    private val jsonBlockRegex = Regex("```(?:json)?\\s*(.*?)```", RegexOption.DOT_MATCHES_ALL)

    /**
     * Texto crudo de Gemini → lista de recomendaciones.
     * Nunca lanza: ante cualquier sorpresa devuelve emptyList().
     */
    fun parse(rawResponse: String): List<GeminiRecommendedDestination> {
        val json = extractJson(rawResponse) ?: return emptyList()

        return try {
            val payload = gson.fromJson(json, GeminiRecommendationResponse::class.java)
            payload?.recommendations
                ?.filter { it.destinationId.isNotBlank() }
                ?.distinctBy { it.destinationId }
                ?: emptyList()
        } catch (_: JsonSyntaxException) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Aísla el primer objeto JSON { ... } del texto recibido. */
    internal fun extractJson(rawResponse: String): String? {
        val text = rawResponse.trim()
        if (text.isEmpty()) return null

        // 1. Bloque markdown ```json ... ```
        jsonBlockRegex.find(text)?.let { match ->
            return match.groupValues[1].trim().takeIf { it.startsWith("{") }
        }

        // 2. Primer { ... } balanceado (tolera texto antes/después)
        val start = text.indexOf('{')
        if (start < 0) return null

        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until text.length) {
            val char = text[index]
            when {
                escaped -> escaped = false
                char == '\\' -> escaped = true
                char == '"' -> inString = !inString
                !inString && char == '{' -> depth++
                !inString && char == '}' -> {
                    depth--
                    if (depth == 0) return text.substring(start, index + 1)
                }
            }
        }
        return null
    }
}
