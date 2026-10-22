package com.example.perutours.data.remote.geoapify

import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP de la API externa de Geoapify.
 *
 * Mantiene la política de caché en disco (10 MB) y los timeouts del cliente
 * anterior para soportar consultas repetidas sin salir a la red.
 */
object GeoapifyClient {

    // URL base del servicio externo de lugares (Geoapify).
    const val BASE_URL = "https://api.geoapify.com/"

    @Volatile
    private var apiService: GeoapifyApiService? = null

    fun getApiService(cacheDirectory: File): GeoapifyApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildService(cacheDirectory).also { apiService = it }
        }
    }

    private fun buildService(cacheDirectory: File): GeoapifyApiService {
        val cache = Cache(File(cacheDirectory, "geoapify_cache"), 10L * 1024L * 1024L)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            // Caché en disco de 1 hora para no repetir consultas de lugares.
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=3600")
                    .removeHeader("Pragma")
                    .build()
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeoapifyApiService::class.java)
    }
}
