package com.example.perutours.ui.screens.request

import com.example.perutours.data.model.TravelPackageSnapshot
import com.example.perutours.data.model.CatalogService
import com.example.perutours.data.model.TourDestination
import com.example.perutours.data.model.TravelRequestType

data class TravelRequestUiState(
    val requestType: String = TravelRequestType.CUSTOM.wireValue,
    val packageId: String = "",
    val destinationId: String = "",
    val packageSnapshot: TravelPackageSnapshot = TravelPackageSnapshot(),
    val catalogDestination: TourDestination? = null,
    val destinationSuggestions: List<TourDestination> = emptyList(),
    val availableActivities: List<CatalogService> = emptyList(),
    val requestedServiceIds: List<String> = emptyList(),
    val isLoadingActivities: Boolean = false,
    val activityLoadFailed: Boolean = false,
    val destination: String = "",
    val destinationLatitude: Double? = null,
    val destinationLongitude: Double? = null,
    val originCity: String = "",
    val originLatitude: Double? = null,
    val originLongitude: Double? = null,
    val departureAtMillis: Long = 0L,
    val returnAtMillis: Long = 0L,
    val travelerCount: Int = 1,
    val notes: String = "",
    val destinationError: String? = null,
    val originCityError: String? = null,
    val departureError: String? = null,
    val returnError: String? = null,
    val travelerCountError: String? = null,
    val isLocating: Boolean = false,
    val isLoadingPackage: Boolean = false,
    val isSaving: Boolean = false,
    val savedRequestId: String? = null,
    val message: String? = null
) {
    val isPackageRequest: Boolean
        get() = requestType == TravelRequestType.PACKAGE.wireValue && packageId.isNotBlank()
}
