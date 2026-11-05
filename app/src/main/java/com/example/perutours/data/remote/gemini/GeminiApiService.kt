package com.example.perutours.data.remote.gemini

import com.example.perutours.BuildConfig
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API REST de Gemini para la HU10 (servicio de recomendación inteligente).
 *
 * La clave jamás se escribe en el código: llega como query param desde
 * BuildConfig.GEMINI_API_KEY (local.properties), igual que Geoapify.
 */
interface GeminiApiService {

    @POST("models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String = BuildConfig.GEMINI_API_KEY,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse
}
