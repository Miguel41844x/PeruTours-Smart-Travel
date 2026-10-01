package com.example.perutours.ui.screens.home

import com.example.perutours.data.remote.DestinationDto

// Criterio 2: Manejo reactivo de estados con sealed class
sealed class HomeDestinationsState {
    object Loading : HomeDestinationsState()
    data class Success(val destinations: List<DestinationDto>, val fromCache: Boolean = false) : HomeDestinationsState()
    data class Error(val message: String) : HomeDestinationsState()
}

data class HomeUiState(
    val userName: String = "Viajero",
    val destinationsState: HomeDestinationsState = HomeDestinationsState.Loading,
    val selectedCategory: String = "Todos"
)