package com.example.perutours.data.remote.gemini

import com.example.perutours.BuildConfig
import kotlinx.coroutines.CancellationException

/**
 * Puerta única de Gemini para toda la app (HU10 recomendaciones y
 * HU11 chatbot), alineada con el codelab de Firebase AI Logic.
 *
 * Orden de intentos:
 *  1. **SDK de Firebase AI Logic** ([GeminiSdkGateway]): no necesita la
 *     clave de Gemini porque la autenticación la resuelve Firebase con
 *     google-services.json.
 *  2. **Cliente REST existente** ([GeminiApiService] + [GeminiClient]):
 *     solo si el SDK falló y hay clave en BuildConfig (local.properties).
 *     Es el mismo código de siempre, con su modelo `gemini-3.6-flash`.
 *  3. Si no hay clave REST y el SDK falló, se propaga la excepción del
 *     SDK: los repositorios aplican su respaldo local (LocalRecommender)
 *     o su mapeo de fallos (ChatRepository.mapFailure).
 *
 * Si el REST también falla, se propaga la excepción del REST
 * (HttpException/IOException), que es la que mapFailure ya sabe traducir.
 */
class GeminiGateway(
    private val sdk: GeminiSdkGateway,
    private val rest: GeminiApiService
) {

    /**
     * Genera contenido para [request].
     *
     * @param apiKey clave REST de respaldo (vacía = sin respaldo posible).
     * @param restModel modelo del cliente REST de respaldo.
     */
    suspend fun generate(
        request: GeminiGenerateRequest,
        apiKey: String = BuildConfig.GEMINI_API_KEY,
        restModel: String = GeminiClient.MODEL
    ): GeminiGenerateResponse {
        val sdkFailure = try {
            return sdk.generate(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e
        }

        // Guard de clave vacía (antes en los repositorios): sin clave no
        // tiene sentido llamar al REST; se devuelve el fallo real del SDK.
        if (apiKey.isBlank()) throw sdkFailure

        return rest.generateContent(
            model = restModel,
            apiKey = apiKey,
            request = request
        )
    }
}
