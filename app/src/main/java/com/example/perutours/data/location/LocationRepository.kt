package com.example.perutours.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

data class DetectedLocation(
    val city: String,
    val latitude: Double,
    val longitude: Double
)

data class ResolvedCity(
    val displayName: String,
    val latitude: Double,
    val longitude: Double
)

class LocationRepository(context: Context) {
    private val appContext = context.applicationContext
    private val locationClient = LocationServices.getFusedLocationProviderClient(appContext)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): DetectedLocation {
        val cancellationTokenSource = CancellationTokenSource()
        val location = locationClient
            .getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            )
            .await()
            ?: throw IllegalStateException("No se pudo obtener la ubicación actual.")

        return DetectedLocation(
            city = resolveCityFromCoordinates(location.latitude, location.longitude),
            latitude = location.latitude,
            longitude = location.longitude
        )
    }

    @Suppress("DEPRECATION")
    private suspend fun resolveCityFromCoordinates(latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            val address = Geocoder(appContext, Locale.getDefault())
                .getFromLocation(latitude, longitude, 1)
                ?.firstOrNull()

            address?.locality
                ?: address?.subAdminArea
                ?: address?.adminArea
                ?: ""
        }
    }

    @Suppress("DEPRECATION")
    suspend fun findCity(query: String): ResolvedCity? {
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < 2) return null
        if (!Geocoder.isPresent()) {
            throw IllegalStateException("El servicio de verificación de ciudades no está disponible.")
        }

        return withContext(Dispatchers.IO) {
            val address = Geocoder(appContext, Locale.getDefault())
                .getFromLocationName(normalizedQuery, 5)
                ?.firstOrNull { candidate ->
                    !candidate.locality.isNullOrBlank() ||
                        !candidate.subAdminArea.isNullOrBlank() ||
                        !candidate.adminArea.isNullOrBlank()
                }
                ?: return@withContext null

            val cityName = address.locality
                ?: address.subAdminArea
                ?: address.adminArea
                ?: return@withContext null
            val countryName = address.countryName.orEmpty()
            val displayName = listOf(cityName, countryName)
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase(Locale.getDefault()) }
                .joinToString(", ")

            ResolvedCity(
                displayName = displayName,
                latitude = address.latitude,
                longitude = address.longitude
            )
        }
    }
}
