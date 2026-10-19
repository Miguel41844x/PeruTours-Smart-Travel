package com.example.perutours.ui.screens.request

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.location.LocationDataSource
import com.example.perutours.data.model.TravelRequest
import com.example.perutours.data.repository.TravelRequestDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TravelRequestViewModel(
    application: Application,
    private val requestRepository: TravelRequestDataSource,
    private val locationRepository: LocationDataSource,
    private val placeResolver: TravelRequestPlaceResolver
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(TravelRequestUiState())
    val uiState: StateFlow<TravelRequestUiState> = _uiState.asStateFlow()

    fun onDestinationChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            destination = value.take(MAX_PLACE_LENGTH),
            destinationError = null
        )
    }

    fun onOriginCityChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            originCity = value.take(MAX_PLACE_LENGTH),
            originLatitude = null,
            originLongitude = null,
            originCityError = null
        )
    }

    fun onDepartureChanged(value: Long) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            departureAtMillis = value,
            returnAtMillis = currentState.returnAtMillis.takeIf { it > value } ?: 0L,
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

    fun prefillFromPackage(
        destination: String,
        packageTitle: String,
        duration: String,
        description: String
    ) {
        if (destination.isBlank() || packageTitle.isBlank()) return

        val state = _uiState.value
        if (state.destination.isNotBlank() || state.notes.isNotBlank()) return

        val referenceNote = buildString {
            append("Paquete de referencia: ")
            append(packageTitle.trim())
            if (duration.isNotBlank()) append(" (${duration.trim()})")
            if (description.isNotBlank()) append(". ${description.trim()}")
        }.take(MAX_NOTES_LENGTH)

        _uiState.value = state.copy(
            destination = destination.trim().take(MAX_PLACE_LENGTH),
            notes = referenceNote,
            destinationError = null,
            message = "Usamos $packageTitle como referencia para tu solicitud."
        )
    }

    fun detectCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLocating = true, message = null)

            try {
                val location = locationRepository.getCurrentLocation()
                _uiState.value = _uiState.value.copy(
                    originCity = location.city,
                    originLatitude = location.latitude,
                    originLongitude = location.longitude,
                    originCityError = null,
                    isLocating = false,
                    message = if (location.city.isBlank()) {
                        "Ubicación detectada. Escribe tu ciudad de origen."
                    } else {
                        "Ciudad de origen detectada automáticamente."
                    }
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLocating = false,
                    message = "No pudimos detectar tu ubicación. Puedes escribir la ciudad manualmente."
                )
            }
        }
    }

    fun onLocationPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            message = "Necesitamos permiso de ubicación para detectar la ciudad de origen."
        )
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

    fun submitRequest() {
        if (!validate()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, message = null)

            try {
                var state = _uiState.value
                val places = when (
                    val result = placeResolver.resolve(
                        destinationQuery = state.destination,
                        originCity = state.originCity,
                        originLatitude = state.originLatitude,
                        originLongitude = state.originLongitude
                    )
                ) {
                    PlaceResolutionResult.InvalidDestination -> {
                        _uiState.value = state.copy(
                            destinationError = "No encontramos ese destino. Selecciona un lugar real.",
                            isSaving = false,
                            message = "Revisa el destino ingresado."
                        )
                        return@launch
                    }
                    PlaceResolutionResult.InvalidOrigin -> {
                        _uiState.value = state.copy(
                            originCityError = "No encontramos esa ciudad de origen.",
                            isSaving = false,
                            message = "Revisa la ciudad de origen ingresada."
                        )
                        return@launch
                    }
                    is PlaceResolutionResult.Success -> result
                }

                state = state.copy(
                    destination = places.destination.city
                        .take(TravelRequestValidator.MAX_PLACE_LENGTH),
                    originCity = places.origin.city
                        .take(TravelRequestValidator.MAX_PLACE_LENGTH),
                    originLatitude = places.origin.latitude,
                    originLongitude = places.origin.longitude
                )
                _uiState.value = state.copy(isSaving = true)

                val savedRequest = requestRepository.save(
                    TravelRequest(
                        destination = state.destination.trim(),
                        destinationLatitude = places.destination.latitude,
                        destinationLongitude = places.destination.longitude,
                        originCity = state.originCity.trim(),
                        originLatitude = state.originLatitude,
                        originLongitude = state.originLongitude,
                        departureAtMillis = state.departureAtMillis,
                        returnAtMillis = state.returnAtMillis,
                        travelerCount = state.travelerCount,
                        notes = state.notes.trim()
                    )
                )

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedRequestId = savedRequest.id
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    message = "No pudimos enviar la solicitud. Revisa tu conexión e inténtalo nuevamente."
                )
            }
        }
    }

    fun clearSavedRequest() {
        _uiState.value = _uiState.value.copy(savedRequestId = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    companion object {
        private const val MAX_PLACE_LENGTH = 120
        private const val MAX_NOTES_LENGTH = 500
    }
}
