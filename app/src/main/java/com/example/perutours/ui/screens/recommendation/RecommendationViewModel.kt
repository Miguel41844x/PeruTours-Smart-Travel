package com.example.perutours.ui.screens.recommendation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.repository.RecommendationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * HU10: ViewModel de la sección "Recomendado para ti" (MVVM).
 *
 * Expone un flujo de estados sellados (Loading / Success / Empty / Error)
 * y delega toda la lógica de datos en RecommendationRepository
 * (preferencias + IA Gemini + caché Firestore + API externa).
 */
class RecommendationViewModel(
    private val repository: RecommendationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecommendationUiState>(RecommendationUiState.Loading)
    val uiState: StateFlow<RecommendationUiState> = _uiState.asStateFlow()

    init {
        loadRecommendations()
    }

    fun loadRecommendations(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Mientras no haya resultado se muestra el indicador de carga.
            if (_uiState.value !is RecommendationUiState.Success) {
                _uiState.value = RecommendationUiState.Loading
            }

            val result = repository.getRecommendations(forceRefresh = forceRefresh)

            _uiState.value = result.fold(
                onSuccess = { recommendations ->
                    if (recommendations.isEmpty()) {
                        RecommendationUiState.Empty
                    } else {
                        RecommendationUiState.Success(recommendations)
                    }
                },
                onFailure = {
                    RecommendationUiState.Error(
                        "No pudimos generar tus recomendaciones. Revisa tu conexión."
                    )
                }
            )
        }
    }

    /** Reintenta llamando a la IA ignorando la caché de Firestore. */
    fun refresh() {
        loadRecommendations(forceRefresh = true)
    }
}
