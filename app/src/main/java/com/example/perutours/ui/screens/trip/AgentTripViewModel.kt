package com.example.perutours.ui.screens.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.model.TripIncident
import com.example.perutours.data.model.TripItineraryItem
import com.example.perutours.data.repository.AgentTripDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgentTripUiState(
    val trips: List<Reservation> = emptyList(),
    val selectedTrip: Reservation? = null,
    val itinerary: List<TripItineraryItem> = emptyList(),
    val incidents: List<TripIncident> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val itemInProgress: String? = null,
    val message: String? = null
)

class AgentTripViewModel(
    private val repository: AgentTripDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentTripUiState())
    val uiState: StateFlow<AgentTripUiState> = _uiState.asStateFlow()

    private var itineraryJob: Job? = null
    private var incidentJob: Job? = null

    init {
        observeTrips()
    }

    fun loadTrip(reservationId: String) {
        if (reservationId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, message = "No se recibió la reserva.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            runCatching { repository.getAssignedTrip(reservationId) }
                .onSuccess { reservation ->
                    _uiState.update { it.copy(selectedTrip = reservation, isLoading = false) }
                    observeTripData(reservationId)
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = error.localizedMessage ?: "No pudimos abrir este viaje."
                        )
                    }
                }
        }
    }

    fun saveItinerary(draft: AgentItineraryDraft, existing: TripItineraryItem? = null) {
        val reservation = _uiState.value.selectedTrip ?: return
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            runCatching { AgentItineraryForm.toModel(draft, reservation, existing) }
                .mapCatching { repository.saveItineraryItem(it) }
                .onSuccess {
                    _uiState.update { it.copy(message = "La actividad quedó guardada.") }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(message = error.localizedMessage ?: "No pudimos guardar la actividad.")
                    }
                }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun deleteItinerary(itemId: String) {
        runItemAction(itemId) { repository.deleteItineraryItem(itemId) }
    }

    fun respondToIncident(incidentId: String, response: String, status: String) {
        runItemAction(incidentId) {
            repository.respondToIncident(incidentId, response, status)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun observeTrips() {
        viewModelScope.launch {
            try {
                repository.observeAssignedTrips().collect { trips ->
                    _uiState.update { it.copy(trips = trips, isLoading = false) }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = error.localizedMessage ?: "No pudimos cargar los viajes asignados."
                    )
                }
            }
        }
    }

    private fun observeTripData(reservationId: String) {
        itineraryJob?.cancel()
        incidentJob?.cancel()
        itineraryJob = viewModelScope.launch {
            repository.observeItinerary(reservationId).collect { itinerary ->
                _uiState.update { it.copy(itinerary = itinerary) }
            }
        }
        incidentJob = viewModelScope.launch {
            repository.observeIncidents(reservationId).collect { incidents ->
                _uiState.update { it.copy(incidents = incidents) }
            }
        }
    }

    private fun runItemAction(itemId: String, action: suspend () -> Unit) {
        if (itemId.isBlank() || _uiState.value.itemInProgress != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(itemInProgress = itemId, message = null) }
            runCatching { action() }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _uiState.update {
                        it.copy(message = error.localizedMessage ?: "No pudimos completar la acción.")
                    }
                }
            _uiState.update { it.copy(itemInProgress = null) }
        }
    }
}
