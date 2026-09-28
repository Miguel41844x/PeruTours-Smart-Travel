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
            city = resolveCity(location.latitude, location.longitude),
            latitude = location.latitude,
            longitude = location.longitude
        )
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
}
