package com.example.perutours.ui.screens.request

data class TravelRequestUiState(
    val destination: String = "",
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
    val isValidatingOrigin: Boolean = false,
    val isOriginVerified: Boolean = false,
    val isSaving: Boolean = false,
    val savedRequestId: String? = null,
    val message: String? = null
)
