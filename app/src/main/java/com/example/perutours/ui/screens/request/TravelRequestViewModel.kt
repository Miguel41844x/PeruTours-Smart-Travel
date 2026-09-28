package com.example.perutours.ui.screens.request

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.location.LocationRepository
import com.example.perutours.data.model.TravelRequest
import com.example.perutours.data.repository.TravelRequestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TravelRequestViewModel(
    application: Application,
    private val requestRepository: TravelRequestRepository
) : AndroidViewModel(application) {
    constructor(application: Application) : this(
        application = application,
        requestRepository = TravelRequestRepository()
    )

    private val locationRepository = LocationRepository(application)
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
            isOriginVerified = false,
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

    fun detectCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLocating = true, message = null)

            try {
                val location = locationRepository.getCurrentLocation()
                _uiState.value = _uiState.value.copy(
                    originCity = location.city,
                    originLatitude = location.latitude,
                    originLongitude = location.longitude,
                    isOriginVerified = location.city.isNotBlank(),
                    originCityError = null,
                    isLocating = false,
                    message = if (location.city.isBlank()) {
                        "Ubicación detectada. Escribe tu ciudad de origen."
                    } else {
                        "Ciudad de origen detectada automáticamente."
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLocating = false,
                    message = e.localizedMessage ?: "No se pudo detectar tu ubicación."
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
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                isValidatingOrigin = !_uiState.value.isOriginVerified,
                message = null
            )

            try {
                val state = _uiState.value
                val needsOriginVerification = !state.isOriginVerified ||
                    state.originLatitude == null ||
                    state.originLongitude == null
                val resolvedOrigin = if (needsOriginVerification) {
                    locationRepository.findCity(state.originCity)
                } else {
                    null
                }

                if (needsOriginVerification && resolvedOrigin == null) {
                    _uiState.value = state.copy(
                        isSaving = false,
                        isValidatingOrigin = false,
                        originCityError = "No encontramos esa ciudad. Revisa el nombre o usa tu ubicación."
                    )
                    return@launch
                }

                val verifiedOriginCity = resolvedOrigin?.displayName ?: state.originCity.trim()
                val verifiedLatitude = resolvedOrigin?.latitude ?: state.originLatitude
                val verifiedLongitude = resolvedOrigin?.longitude ?: state.originLongitude

                _uiState.value = state.copy(
                    originCity = verifiedOriginCity,
                    originLatitude = verifiedLatitude,
                    originLongitude = verifiedLongitude,
                    originCityError = null,
                    isOriginVerified = true,
                    isValidatingOrigin = false
                )

                val savedRequest = requestRepository.save(
                    TravelRequest(
                        destination = state.destination.trim(),
                        originCity = verifiedOriginCity,
                        originLatitude = verifiedLatitude,
                        originLongitude = verifiedLongitude,
                        departureAtMillis = state.departureAtMillis,
                        returnAtMillis = state.returnAtMillis,
                        travelerCount = state.travelerCount,
                        notes = state.notes.trim()
                    )
                )

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isValidatingOrigin = false,
                    savedRequestId = savedRequest.id
                )
            } catch (e: Exception) {
                val originWasVerified = _uiState.value.isOriginVerified
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isValidatingOrigin = false,
                    originCityError = if (originWasVerified) {
                        _uiState.value.originCityError
                    } else {
                        "No se pudo verificar la ciudad en este momento."
                    },
                    message = if (originWasVerified) {
                        "No se pudo guardar la solicitud: ${e.localizedMessage.orEmpty()}"
                    } else {
                        e.localizedMessage ?: "No se pudo verificar la ciudad en este momento."
                    }
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
}
