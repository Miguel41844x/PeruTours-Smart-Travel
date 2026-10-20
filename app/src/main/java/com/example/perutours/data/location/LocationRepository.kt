package com.example.perutours.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
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

interface LocationDataSource {
    suspend fun getCurrentLocation(): DetectedLocation
    suspend fun findPlace(query: String): DetectedLocation?
}

class LocationRepository(context: Context) : LocationDataSource {
    private val appContext = context.applicationContext
    private val locationClient = LocationServices.getFusedLocationProviderClient(appContext)

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): DetectedLocation {
        val cancellationTokenSource = CancellationTokenSource()
        val location = locationClient
            .getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            )
            .await()
            ?: throw IllegalStateException("No se pudo obtener la ubicación actual.")

        return DetectedLocation(
            city = resolveCity(location.latitude, location.longitude),
            latitude = location.latitude,
            longitude = location.longitude
        )
    }

    @Suppress("DEPRECATION")
    override suspend fun findPlace(query: String): DetectedLocation? {
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < 2) return null
        if (!Geocoder.isPresent()) {
            throw IllegalStateException("El servicio de búsqueda de lugares no está disponible.")
        }

        return withContext(Dispatchers.IO) {
            Geocoder(appContext, PERU_LOCALE)
                .getFromLocationName(
                    "$normalizedQuery, Perú",
                    MAX_GEOCODER_RESULTS,
                    PERU_SOUTH_LATITUDE,
                    PERU_WEST_LONGITUDE,
                    PERU_NORTH_LATITUDE,
                    PERU_EAST_LONGITUDE
                )
                ?.firstOrNull { address ->
                    address.countryCode.equals(PERU_COUNTRY_CODE, ignoreCase = true) &&
                        address.hasMeaningfulPlaceName()
                }
                ?.let { address ->
                    DetectedLocation(
                        city = address.toPlaceLabel(normalizedQuery),
                        latitude = address.latitude,
                        longitude = address.longitude
                    )
                }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun resolveCity(latitude: Double, longitude: Double): String {
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

    private fun Address.hasMeaningfulPlaceName(): Boolean {
        return !locality.isNullOrBlank() ||
            !subAdminArea.isNullOrBlank() ||
            !adminArea.isNullOrBlank() ||
            !countryName.isNullOrBlank() ||
            !featureName.isNullOrBlank()
    }

    private fun Address.toPlaceLabel(fallback: String): String {
        val parts = listOf(locality, subAdminArea, adminArea, countryName)
            .filterNotNull()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        return parts.take(3).joinToString(", ")
            .ifBlank { featureName?.trim().orEmpty() }
            .ifBlank { fallback }
    }

    private companion object {
        const val MAX_GEOCODER_RESULTS = 5
        const val PERU_COUNTRY_CODE = "PE"
        const val PERU_SOUTH_LATITUDE = -18.5
        const val PERU_WEST_LONGITUDE = -81.5
        const val PERU_NORTH_LATITUDE = 0.1
        const val PERU_EAST_LONGITUDE = -68.5

        val PERU_LOCALE: Locale = Locale.Builder()
            .setLanguage("es")
            .setRegion(PERU_COUNTRY_CODE)
            .build()
    }
}
