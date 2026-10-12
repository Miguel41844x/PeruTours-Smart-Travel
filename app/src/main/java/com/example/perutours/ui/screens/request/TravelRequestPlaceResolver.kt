package com.example.perutours.ui.screens.request

import com.example.perutours.data.location.DetectedLocation
import com.example.perutours.data.location.LocationDataSource

sealed interface PlaceResolutionResult {
    data class Success(
        val destination: DetectedLocation,
        val origin: DetectedLocation
    ) : PlaceResolutionResult

    data object InvalidDestination : PlaceResolutionResult
    data object InvalidOrigin : PlaceResolutionResult
}

class TravelRequestPlaceResolver(
    private val locationDataSource: LocationDataSource
) {
    suspend fun resolve(
        destinationQuery: String,
        originCity: String,
        originLatitude: Double?,
        originLongitude: Double?
    ): PlaceResolutionResult {
        val destination = locationDataSource.findPlace(destinationQuery)
            ?: return PlaceResolutionResult.InvalidDestination

        val origin = if (originLatitude != null && originLongitude != null) {
            DetectedLocation(
                city = originCity.trim(),
                latitude = originLatitude,
                longitude = originLongitude
            )
        } else {
            locationDataSource.findPlace(originCity)
                ?: return PlaceResolutionResult.InvalidOrigin
        }

        return PlaceResolutionResult.Success(
            destination = destination,
            origin = origin
        )
    }
}
