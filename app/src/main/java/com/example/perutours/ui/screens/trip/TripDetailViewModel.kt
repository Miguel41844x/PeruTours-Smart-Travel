package com.example.perutours.ui.screens.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.repository.TripDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TripDetailViewModel(
    private val repository: TripDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripDetailUiState())
    val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

    private var observationJob: Job? = null

    fun load(reservationId: String) {
        if (reservationId.isBlank()) {
            _uiState.update {
                it.copy(isLoading = false, message = "No se recibió el viaje que deseas abrir.")
            }
            return
        }
        if (_uiState.value.reservationId == reservationId && observationJob?.isActive == true) {
            return
        }

        observationJob?.cancel()
        _uiState.value = TripDetailUiState(reservationId = reservationId)
        observationJob = viewModelScope.launch {
            try {
                repository.observeTrip(reservationId).collect { overview ->
                    _uiState.update {
                        it.copy(
                            overview = overview,
                            isLoading = overview == null && it.isRefreshing,
                            message = if (overview != null) null else it.message
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = error.localizedMessage ?: "No pudimos abrir este viaje."
                    )
                }
            }
        }
        refresh()
        repository.requestChecklistSync()
    }

    fun refresh() {
        val reservationId = _uiState.value.reservationId
        if (reservationId.isBlank() || _uiState.value.isRefreshing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, message = null) }
            repository.refreshTrip(reservationId)
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _uiState.update {
                        it.copy(
                            message = if (it.overview == null) {
                                error.localizedMessage ?: "No pudimos cargar este viaje."
                            } else {
                                "No se pudo actualizar. Conservamos la última información guardada."
                            }
                        )
                    }
                }
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    isLoading = it.overview == null
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun addChecklistItem(title: String, category: String) {
        val reservationId = _uiState.value.reservationId
        if (reservationId.isBlank() || _uiState.value.isSavingChecklist) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingChecklist = true, message = null) }
            repository.addChecklistItem(reservationId, title, category)
                .onFailure { error ->
                    _uiState.update {
                        it.copy(message = error.localizedMessage ?: "No pudimos agregar el pendiente.")
                    }
                }
            _uiState.update { it.copy(isSavingChecklist = false) }
        }
    }

    fun toggleChecklistItem(itemId: String) {
        runChecklistAction(itemId) { repository.toggleChecklistItem(itemId) }
    }

    fun deleteChecklistItem(itemId: String) {
        runChecklistAction(itemId) { repository.deleteChecklistItem(itemId) }
    }

    fun syncChecklist() = repository.requestChecklistSync()

    private fun runChecklistAction(
        itemId: String,
        action: suspend () -> Result<Unit>
    ) {
        if (itemId.isBlank() || _uiState.value.checklistItemInProgress != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(checklistItemInProgress = itemId, message = null) }
            action().onFailure { error ->
                _uiState.update {
                    it.copy(message = error.localizedMessage ?: "No pudimos actualizar el checklist.")
                }
            }
            _uiState.update { it.copy(checklistItemInProgress = null) }
        }
    }
}
