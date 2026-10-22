package com.example.perutours.ui.screens.trip

import com.example.perutours.data.model.TripOverview
import com.example.perutours.data.location.DetectedLocation

data class TripDetailUiState(
    val reservationId: String = "",
    val overview: TripOverview? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSavingChecklist: Boolean = false,
    val checklistItemInProgress: String? = null,
    val isSubmittingIncident: Boolean = false,
    val isDetectingLocation: Boolean = false,
    val incidentLocation: DetectedLocation? = null,
    val message: String? = null
)

enum class TripDetailSection(val label: String) {
    ITINERARY("Itinerario"),
    CHECKLIST("Checklist"),
    SUPPORT("Ayuda")
}
