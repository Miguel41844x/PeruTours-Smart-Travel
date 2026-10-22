package com.example.perutours.ui.screens.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.repository.TripDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TripListViewModel(
    private val repository: TripDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripListUiState())
    val uiState: StateFlow<TripListUiState> = _uiState.asStateFlow()

    init {
        observeTrips()
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshing = true,
                    errorMessage = null
                )
            }
            repository.refreshTrips()
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _uiState.update {
                        it.copy(
                            errorMessage = if (it.trips.isEmpty()) {
                                error.localizedMessage
                                    ?: "No pudimos cargar tus viajes."
                            } else {
                                "Mostramos la información guardada. No se pudo actualizar."
                            }
                        )
                    }
                }
            _uiState.update { it.copy(isRefreshing = false, isLoading = false) }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun observeTrips() {
        viewModelScope.launch {
            try {
                repository.observeTrips().collect { trips ->
                    _uiState.update {
                        it.copy(
                            trips = trips,
                            isLoading = false
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage
                            ?: "No pudimos abrir tus viajes."
                    )
                }
            }
        }
    }
}
