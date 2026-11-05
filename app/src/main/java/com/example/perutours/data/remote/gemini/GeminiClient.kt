package com.example.perutours.data.remote.gemini

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP del servicio de IA Gemini (HU10).
 *
 * Sigue el patrón de GeoapifyClient: singleton perezoso con timeouts
 * propios. Sin caché en disco porque generateContent es POST (no cacheable)
 * y la caché de resultados vive en Firestore (users/{uid}/recommendations).
 */
object GeminiClient {

    // URL base del servicio Gemini (Google AI Studio).
    const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"

    // Modelo gratuito del tier free: rápido y con buena salida en español.
    // Nota: gemini-2.5-flash dejó de estar disponible para usuarios nuevos
    // y los modelos 3.7/3.8 estaban saturados (503) al momento de probar;
    // gemini-3.6-flash respondió de forma estable.
    const val MODEL = "gemini-3.6-flash"

    @Volatile
    private var apiService: GeminiApiService? = null

    fun getApiService(): GeminiApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildService().also { apiService = it }
        }
    }

    private fun buildService(): GeminiApiService {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            // Las respuestas de un LLM tardan más que una API REST común.
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }
}
