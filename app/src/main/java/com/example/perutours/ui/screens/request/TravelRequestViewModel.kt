package com.example.perutours.ui.screens.request

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TravelRequestViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TravelRequestUiState())
    val uiState: StateFlow<TravelRequestUiState> = _uiState.asStateFlow()

    fun onDestinationChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            destination = value,
            destinationError = null
        )
    }

    fun onOriginCityChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            originCity = value,
            originLatitude = null,
            originLongitude = null,
            originCityError = null
        )
    }

    fun onDepartureChanged(value: Long) {
        _uiState.value = _uiState.value.copy(
            departureAtMillis = value,
            departureError = null,
            returnError = null
        )
    }

    fun onReturnChanged(value: Long) {
        _uiState.value = _uiState.value.copy(
            returnAtMillis = value,
            returnError = null
        )
    }

    fun incrementTravelerCount() {
        val count = (_uiState.value.travelerCount + 1).coerceAtMost(20)
        _uiState.value = _uiState.value.copy(
            travelerCount = count,
            travelerCountError = null
        )
    }

    fun decrementTravelerCount() {
        val count = (_uiState.value.travelerCount - 1).coerceAtLeast(1)
        _uiState.value = _uiState.value.copy(
            travelerCount = count,
            travelerCountError = null
        )
    }

    fun onNotesChanged(value: String) {
        if (value.length <= 500) {
            _uiState.value = _uiState.value.copy(notes = value)
        }
    }

    fun validate(): Boolean {
        val state = _uiState.value
        val validation = TravelRequestValidator.validate(
            destination = state.destination,
            originCity = state.originCity,
            departureAtMillis = state.departureAtMillis,
            returnAtMillis = state.returnAtMillis,
            travelerCount = state.travelerCount
        )

        _uiState.value = state.copy(
            destinationError = validation.destinationError,
            originCityError = validation.originCityError,
            departureError = validation.departureError,
            returnError = validation.returnError,
            travelerCountError = validation.travelerCountError,
            message = if (validation.isValid) {
                null
            } else {
                "Revisa los campos obligatorios de la solicitud."
            }
        )
        return validation.isValid
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
