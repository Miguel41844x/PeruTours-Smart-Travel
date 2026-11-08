package com.example.perutours.domain.recommendation

import com.example.perutours.data.model.TravelPreferences
import com.example.perutours.data.remote.DestinationDto
import com.google.gson.Gson

/**
 * HU10: construye el prompt que se envía a Gemini con las preferencias
 * del usuario y el catálogo de destinos candidatos.
 *
 * El prompt pide EXCLUSIVAMENTE el JSON descrito por
 * GeminiRecommendationResponse y prohíbe inventar destinos: solo puede
 * reordenar los candidatos recibidos.
 */
object RecommendationPromptBuilder {

    private val gson = Gson()

    private const val SYSTEM_RULES = """
Eres el motor de recomendaciones turísticas de PeruTours, una app de viajes por el Perú.
Debes recomendar destinos del catálogo según las preferencias del usuario.

REGLAS ESTRICTAS:
1. Devuelve ÚNICAMENTE un objeto JSON válido con esta forma exacta:
{"recommendations":[{"destinationId":"...","title":"...","reason":"..."}]}
2. Usa EXCLUSIVAMENTE los destinationId del catálogo recibido; jamás inventes destinos.
3. "reason" debe ser una frase corta en español (máximo 120 caracteres) que explique
   por qué ese destino encaja con las preferencias del usuario.
4. Ordena de mejor a peor coincidencia y devuelve máximo 3 recomendaciones.
5. No agregues ningún texto fuera del JSON.
"""

    /** Preferencias + catálogo → texto completo del mensaje del usuario. */
    fun build(
        preferences: TravelPreferences,
        destinations: List<DestinationDto>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("PREFERENCIAS DEL USUARIO:")
        sb.appendLine("- Tipo de viaje: ${preferences.tripType.ifBlank { "no especificado" }}")
        sb.appendLine("- Presupuesto: ${preferences.budget.ifBlank { "no especificado" }}")
        sb.appendLine("- Duración: ${preferences.duration.ifBlank { "no especificado" }}")
        sb.appendLine(
            "- Categorías de interés: " +
                preferences.interestNames.joinToString(", ").ifBlank { "ninguna" }
        )
        sb.appendLine()
        sb.appendLine("CATÁLOGO DE DESTINOS CANDIDATOS (id | título | lugar | duración | precio | categoría):")
        destinations.forEach { destination ->
            sb.appendLine(
                "- ${destination.destinationId.ifBlank { destination.id }} | " +
                    "${destination.title} | ${destination.location} | " +
                    "${destination.duration} | ${destination.price} | ${destination.category}"
            )
        }
        sb.appendLine()
        sb.append("Devuelve el JSON con las recomendaciones.")
        return sb.toString()
    }

    /** Prompt completo (reglas del sistema + mensaje del usuario). */
    fun fullPrompt(
        preferences: TravelPreferences,
        destinations: List<DestinationDto>
    ): String = SYSTEM_RULES.trimIndent() + "\n\n" + build(preferences, destinations)

    /** Payload listo para generateContent con JSON estricto de salida. */
    fun toRequest(
        preferences: TravelPreferences,
        destinations: List<DestinationDto>
    ): com.example.perutours.data.remote.gemini.GeminiGenerateRequest =
        com.example.perutours.data.remote.gemini.GeminiGenerateRequest(
            contents = listOf(
                com.example.perutours.data.remote.gemini.GeminiContent(
                    role = "user",
                    parts = listOf(
                        com.example.perutours.data.remote.gemini.GeminiPart(
                            text = fullPrompt(preferences, destinations)
                        )
                    )
                )
            ),
            generationConfig = com.example.perutours.data.remote.gemini.GeminiGenerationConfig(
                temperature = 0.4,
                maxOutputTokens = 1024,
                responseMimeType = "application/json"
            )
        )

    /** Utilidad interna: serializa un objeto a JSON (usada por el parser). */
    fun toJson(value: Any): String = gson.toJson(value)
}
