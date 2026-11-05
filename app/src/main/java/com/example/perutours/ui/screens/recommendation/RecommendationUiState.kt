package com.example.perutours.ui.screens.recommendation

import com.example.perutours.data.model.Recommendation

/**
 * HU10: estados de la sección "Recomendado para ti" (MVVM + sealed class,
 * mismo patrón que HomeDestinationsState).
 */
sealed class RecommendationUiState {
    object Loading : RecommendationUiState()
    data class Success(val recommendations: List<Recommendation>) : RecommendationUiState()
    object Empty : RecommendationUiState()
    data class Error(val message: String) : RecommendationUiState()
}
