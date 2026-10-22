package com.example.perutours.ui.screens.trip

import com.example.perutours.data.model.Reservation

data class TripListUiState(
    val trips: List<Reservation> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)
