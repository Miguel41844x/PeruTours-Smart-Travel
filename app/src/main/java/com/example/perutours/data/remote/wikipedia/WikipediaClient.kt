package com.example.perutours.data.remote.wikipedia

import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP de la API externa de Wikipedia (información turística).
 *
 * Sigue el patrón de GeoapifyClient: singleton perezoso con caché en
 * disco de 1 hora, de modo que un resumen ya consultado no vuelve a
 * descargarse en las próximas aperturas de la app.
 */
object WikipediaClient {

    // URL base de la API REST de resúmenes (español).
    const val BASE_URL = "https://es.wikipedia.org/api/rest_v1/"

    @Volatile
    private var apiService: WikipediaApiService? = null

    fun getApiService(cacheDirectory: File): WikipediaApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildService(cacheDirectory).also { apiService = it }
        }
    }

    private fun buildService(cacheDirectory: File): WikipediaApiService {
        val cache = Cache(File(cacheDirectory, "wikipedia_cache"), 5L * 1024L * 1024L)

        val okHttpClient = OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            // Resúmenes cacheables en disco por 1 hora (GET).
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
            .create(WikipediaApiService::class.java)
    }
}
