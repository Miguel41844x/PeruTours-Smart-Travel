package com.example.perutours.ui.screens.trip

import com.example.perutours.data.model.TripOverview

data class TripDetailUiState(
    val reservationId: String = "",
    val overview: TripOverview? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val message: String? = null
)

enum class TripDetailSection(val label: String) {
    ITINERARY("Itinerario"),
    CHECKLIST("Checklist"),
    SUPPORT("Ayuda")
}
