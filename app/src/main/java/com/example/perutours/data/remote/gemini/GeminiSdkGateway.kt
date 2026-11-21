package com.example.perutours.data.remote.gemini

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerationConfig
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.HarmBlockThreshold
import com.google.firebase.ai.type.HarmCategory
import com.google.firebase.ai.type.SafetySetting
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig

/**
 * Cliente del SDK de Firebase AI Logic (paso 6 del codelab
 * "Agrega funciones de Gemini a tu app para Android").
 *
 * Inicializa el modelo igual que el codelab:
 * `Firebase.ai(backend = GenerativeBackend.googleAI())` con el modelo
 * `gemini-3.1-flash-lite` y las cuatro `safetySettings` de umbral
 * LOW_AND_ABOVE (HARASSMENT, HATE_SPEECH, SEXUALLY_EXPLICIT y
 * DANGEROUS_CONTENT).
 *
 * Traduce los DTOs REST existentes ([GeminiGenerateRequest] → tipos del
 * SDK, y la respuesta del SDK → [GeminiGenerateResponse]) para que tus
 * prompt builders y el `.text()` de los repositorios sigan intactos.
 * El SDK no necesita la clave de Gemini: la autenticación la resuelve
 * Firebase con google-services.json.
 */
class GeminiSdkGateway {

    /**
     * Genera contenido con el SDK de Firebase AI Logic.
     *
     * @throws com.google.firebase.ai.type.FirebaseAIException si la llamada
     * al modelo falla; quien decide si toca caer al respaldo REST es
     * [GeminiGateway].
     */
    suspend fun generate(request: GeminiGenerateRequest): GeminiGenerateResponse {
        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = MODEL,
                generationConfig = request.generationConfig?.toSdkConfig(),
                safetySettings = SAFETY_SETTINGS,
                systemInstruction = request.systemInstruction?.toSdkContent()
            )

        return model
            .generateContent(request.contents.map { it.toSdkContent() })
            .toSdkResponse()
    }

    // ===================== DTO propio → tipos del SDK =====================

    private fun GeminiContent.toSdkContent(): Content {
        val texts = parts.map { it.text }
        return content(role = role) { texts.forEach { text(it) } }
    }

    private fun GeminiSystemInstruction.toSdkContent(): Content {
        val texts = parts.map { it.text }
        return content { texts.forEach { text(it) } }
    }

    /**
     * Conserva la configuración de tus prompts: maxOutputTokens
     * (1024 recomendaciones / 4096 chat), responseMimeType
     * ("application/json" estricto) y temperature.
     */
    @Suppress("DEPRECATION") // temperature está deprecado en modelos Gemini 3.x
    private fun GeminiGenerationConfig.toSdkConfig(): GenerationConfig {
        val temp = temperature?.toFloat()
        val tokens = maxOutputTokens
        val mime = responseMimeType
        return generationConfig {
            if (temp != null) this.temperature = temp
            if (tokens != null) maxOutputTokens = tokens
            if (mime != null) responseMimeType = mime
        }
    }

    // ===================== SDK → DTO propio =====================

    /**
     * Devuelve el mismo DTO que devolvía el cliente REST para que
     * `response.text()` de los repositorios siga funcionando igual.
     * El `text` del SDK ya concatena las partes de respuesta ignorando
     * las marcadas como "thought" (pensamiento del modelo).
     */
    private fun GenerateContentResponse.toSdkResponse(): GeminiGenerateResponse {
        val text = text ?: return GeminiGenerateResponse(candidates = emptyList())
        return GeminiGenerateResponse(
            candidates = listOf(
                GeminiCandidate(
                    content = GeminiContent(
                        role = "model",
                        parts = listOf(GeminiPart(text))
                    ),
                    finishReason = null
                )
            )
        )
    }

    companion object {
        /** Modelo del codelab: rápido, económico y con cuota gratuita generosa. */
        const val MODEL = GeminiClient.MODEL

        /** Safety settings del codelab: umbral más estricto en las 4 categorías. */
        private val SAFETY_SETTINGS = listOf(
            SafetySetting(HarmCategory.HARASSMENT, HarmBlockThreshold.LOW_AND_ABOVE),
            SafetySetting(HarmCategory.HATE_SPEECH, HarmBlockThreshold.LOW_AND_ABOVE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, HarmBlockThreshold.LOW_AND_ABOVE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, HarmBlockThreshold.LOW_AND_ABOVE)
        )
    }
}
