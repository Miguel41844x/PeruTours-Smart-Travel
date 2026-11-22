package com.example.perutours.ui.screens.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.location.DetectedLocation
import com.example.perutours.data.location.LocationDataSource
import com.example.perutours.data.model.TourDestination
import com.example.perutours.data.model.TravelPackageSnapshot
import com.example.perutours.data.model.TravelRequest
import com.example.perutours.data.model.TravelRequestType
import com.example.perutours.data.repository.DestinationCatalogRepository
import com.example.perutours.data.repository.PackageRepository
import com.example.perutours.data.repository.ServiceCatalogRepository
import com.example.perutours.data.repository.TravelRequestDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

class TravelRequestViewModel(
    private val requestRepository: TravelRequestDataSource,
    private val locationRepository: LocationDataSource,
    private val placeResolver: TravelRequestPlaceResolver,
    private val packageRepository: PackageRepository,
    private val destinationRepository: DestinationCatalogRepository,
    private val serviceCatalogRepository: ServiceCatalogRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TravelRequestUiState())
    val uiState: StateFlow<TravelRequestUiState> = _uiState.asStateFlow()
    private var activitiesJob: Job? = null
    private var publishedDestinations: List<TourDestination>? = null

    fun onDestinationChanged(value: String) {
        if (_uiState.value.isSaving || _uiState.value.isLoadingPackage) return
        activitiesJob?.cancel()
        _uiState.value = _uiState.value.copy(
            requestType = TravelRequestType.CUSTOM.wireValue,
            packageId = "",
            destinationId = "",
            packageSnapshot = TravelPackageSnapshot(),
            catalogDestination = null,
            destinationSuggestions = emptyList(),
            availableActivities = emptyList(),
            requestedServiceIds = emptyList(),
            isLoadingActivities = false,
            activityLoadFailed = false,
            destination = value.take(MAX_PLACE_LENGTH),
            destinationLatitude = null,
            destinationLongitude = null,
            destinationError = null
        )
        loadActivities(debounce = true)
    }

    fun loadActivities(debounce: Boolean = false) {
        activitiesJob?.cancel()
        val form = _uiState.value
        if (form.destination.trim().length < 2) return
        activitiesJob = viewModelScope.launch {
            if (debounce) delay(450)
            _uiState.value = _uiState.value.copy(
                isLoadingActivities = true,
                activityLoadFailed = false
            )
            try {
                val suggestions = if (form.catalogDestination == null) {
                    findCatalogDestinations(form.destination)
                } else {
                    emptyList()
                }
                val destination = form.catalogDestination ?: suggestions.singleOrNull()?.takeIf {
                    normalizeDestination(form.destination) in destinationSearchLabels(it)
                }
                val includedIds = form.packageSnapshot.includedServiceIds
                val activities = destination?.let {
                    serviceCatalogRepository.getRequestableActivities(it.id)
                }.orEmpty().filterNot { it.id in includedIds }
                _uiState.value = _uiState.value.copy(
                    catalogDestination = destination,
                    destinationSuggestions = if (destination == null) suggestions.take(4) else emptyList(),
                    availableActivities = activities,
                    requestedServiceIds = _uiState.value.requestedServiceIds.filter { id ->
                        activities.any { it.id == id }
                    },
                    isLoadingActivities = false
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingActivities = false,
                    activityLoadFailed = true
                )
            }
        }
    }

    private suspend fun findCatalogDestinations(query: String): List<TourDestination> {
        val destinations = publishedDestinations ?: destinationRepository.getPublishedDestinations()
            .also { publishedDestinations = it }
        val input = normalizeDestination(query)
        val matches = destinations.filter { destination ->
            destinationSearchLabels(destination).any { it.startsWith(input) }
        }
        val exactMatches = matches.filter { input in destinationSearchLabels(it) }
        return exactMatches.ifEmpty { matches }.sortedBy(::destinationLabel)
    }

    private fun destinationSearchLabels(destination: TourDestination): List<String> = listOf(
        destination.name,
        "${destination.name}, Perú",
        "${destination.name}, ${destination.department}",
        destinationLabel(destination)
    ).map(::normalizeDestination)

    fun selectCatalogDestination(destination: TourDestination) {
        if (_uiState.value.isSaving || _uiState.value.isPackageRequest) return
        _uiState.value = _uiState.value.copy(
            destinationId = destination.id,
            catalogDestination = destination,
            destinationSuggestions = emptyList(),
            destination = destinationLabel(destination),
            destinationLatitude = destination.latitude,
            destinationLongitude = destination.longitude,
            destinationError = null
        )
        loadActivities()
    }

    fun toggleActivity(serviceId: String) {
        val state = _uiState.value
        if (state.isSaving || state.availableActivities.none { it.id == serviceId }) return
        val selected = state.requestedServiceIds
        val next = when {
            serviceId in selected -> selected - serviceId
            selected.size < state.optionalActivityLimit -> selected + serviceId
            else -> return
        }
        _uiState.value = state.copy(requestedServiceIds = next)
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

    fun prefillFromPackage(packageId: String) {
        if (packageId.isBlank() || _uiState.value.packageId == packageId) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingPackage = true, message = null)
            runCatching {
                val tourPackage = packageRepository.getPackage(packageId)
                    ?.takeIf { it.published }
                    ?: throw NoSuchElementException("El paquete ya no está disponible.")
                val destination = destinationRepository.getDestination(tourPackage.destinationId)
                    ?.takeIf { it.published }
                    ?: throw NoSuchElementException("El destino del paquete ya no está disponible.")
                tourPackage to destination
            }.onSuccess { (tourPackage, destination) ->
                _uiState.value = _uiState.value.copy(
                    requestType = TravelRequestType.PACKAGE.wireValue,
                    packageId = tourPackage.id,
                    destinationId = destination.id,
                    packageSnapshot = TravelPackageSnapshot.from(tourPackage),
                    catalogDestination = destination,
                    destination = destinationLabel(destination),
                    destinationLatitude = destination.latitude,
                    destinationLongitude = destination.longitude,
                    destinationError = null,
                    isLoadingPackage = false,
                    message = "El paquete quedó vinculado a tu solicitud."
                )
                loadActivities()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingPackage = false,
                    message = error.localizedMessage ?: "No pudimos cargar el paquete seleccionado."
                )
            }
        }
    }

    fun prefillFromDestination(destinationId: String) {
        if (
            destinationId.isBlank() ||
            _uiState.value.packageId.isNotBlank() ||
            _uiState.value.destinationId == destinationId
        ) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingPackage = true, message = null)
            runCatching {
                destinationRepository.getDestination(destinationId)
                    ?.takeIf { it.published }
                    ?: throw NoSuchElementException("El destino ya no está disponible.")
            }.onSuccess { destination ->
                _uiState.value = _uiState.value.copy(isLoadingPackage = false)
                selectCatalogDestination(destination)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingPackage = false,
                    message = error.localizedMessage ?: "No pudimos cargar el destino seleccionado."
                )
            }
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
        if (_uiState.value.isSaving || _uiState.value.isLoadingPackage) return
        if (!validate()) return

        val form = _uiState.value
        _uiState.value = form.copy(isSaving = true, message = null)
        viewModelScope.launch {
            try {
                var state = form
                val catalogDestination = state.catalogDestination
                val places = when (
                    val result = placeResolver.resolve(
                        destinationQuery = state.destination,
                        originCity = state.originCity,
                        originLatitude = state.originLatitude,
                        originLongitude = state.originLongitude,
                        knownDestination = catalogDestination?.let {
                            DetectedLocation(destinationLabel(it), it.latitude, it.longitude)
                        }
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

                val resolvedDestination = if (state.isPackageRequest) {
                    state.destination
                } else {
                    places.destination.city.take(TravelRequestValidator.MAX_PLACE_LENGTH)
                }
                val resolvedDestinationLatitude = if (state.isPackageRequest) {
                    state.destinationLatitude
                } else {
                    places.destination.latitude
                }
                val resolvedDestinationLongitude = if (state.isPackageRequest) {
                    state.destinationLongitude
                } else {
                    places.destination.longitude
                }

                state = state.copy(
                    destinationId = catalogDestination?.id ?: state.destinationId,
                    destination = resolvedDestination,
                    destinationLatitude = resolvedDestinationLatitude,
                    destinationLongitude = resolvedDestinationLongitude,
                    originCity = places.origin.city
                        .take(TravelRequestValidator.MAX_PLACE_LENGTH),
                    originLatitude = places.origin.latitude,
                    originLongitude = places.origin.longitude
                )
                _uiState.value = state.copy(isSaving = true)

                val savedRequest = requestRepository.save(
                    TravelRequest(
                        requestType = state.requestType,
                        packageId = state.packageId,
                        destinationId = state.destinationId,
                        packageSnapshot = state.packageSnapshot,
                        requestedServiceIds = state.requestedServiceIds,
                        destination = state.destination.trim(),
                        destinationLatitude = state.destinationLatitude,
                        destinationLongitude = state.destinationLongitude,
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

    private fun destinationLabel(destination: TourDestination): String =
        listOf(destination.name, destination.department, "Perú")
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(", ")
            .take(MAX_PLACE_LENGTH)

    private fun normalizeDestination(value: String): String =
        Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)

    companion object {
        private const val MAX_PLACE_LENGTH = 120
    }
}
