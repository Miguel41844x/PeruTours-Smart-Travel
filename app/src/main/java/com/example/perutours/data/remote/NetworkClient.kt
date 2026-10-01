package com.example.perutours.data.remote

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object NetworkClient {

    // URL base de la API externa (puedes apuntarla a tu endpoint de GitHub Pages o MockAPI)
    private const val BASE_URL = "https://raw.githubusercontent.com/Miguel41844x/PeruTours-Smart-Travel/main/api/"

    private var apiService: DestinationsApiService? = null

    fun getApiService(context: Context): DestinationsApiService {
        if (apiService == null) {
            // Criterio 1: Configuración de caché en disco (10 MB)
            val cacheDirectory = File(context.cacheDir, "http_cache")
            val cache = Cache(cacheDirectory, 10L * 1024L * 1024L)

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // Cliente OkHttp con Caché activa e interceptor de expiración
            val okHttpClient = OkHttpClient.Builder()
                .cache(cache)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                // Interceptor de red para forzar almacenamiento en caché local (1 hora)
                .addNetworkInterceptor { chain ->
                    val response = chain.proceed(chain.request())
                    response.newBuilder()
                        .header("Cache-Control", "public, max-age=3600")
                        .removeHeader("Pragma")
                        .build()
                }
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit.create(DestinationsApiService::class.java)
        }
        return apiService!!
    }
}
