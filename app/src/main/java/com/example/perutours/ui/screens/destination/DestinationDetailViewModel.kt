package com.example.perutours.ui.screens.destination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.TourDestination
import com.example.perutours.data.model.TourPackage
import com.example.perutours.data.repository.DestinationCatalogRepository
import com.example.perutours.data.repository.PackageRepository
import com.example.perutours.data.repository.PointsOfInterestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DestinationDetailUiState(
    val isLoading: Boolean = true,
    val destination: TourDestination? = null,
    val packages: List<TourPackage> = emptyList(),
    val places: List<PointsOfInterestRepository.NearbyPlace> = emptyList(),
    val arePlacesLoading: Boolean = false,
    val arePlacesFromCache: Boolean = false,
    val errorMessage: String? = null,
    val placesErrorMessage: String? = null
)

/**
 * Detalle del destino para el cliente.
 *
 * Carga el destino y sus paquetes PUBLICADOS, y consulta los puntos de
 * interés cercanos a través de la API externa de Geoapify (cacheados en
 * Room).
 */
class DestinationDetailViewModel(
    private val destinationRepository: DestinationCatalogRepository,
    private val packageRepository: PackageRepository,
    private val pointsOfInterestRepository: PointsOfInterestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DestinationDetailUiState())
    val uiState: StateFlow<DestinationDetailUiState> = _uiState.asStateFlow()

    private var loadedDestinationId: String? = null

    fun load(destinationId: String) {
        if (destinationId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Destino no encontrado.") }
            return
        }
        if (loadedDestinationId == destinationId) return
        loadedDestinationId = destinationId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val destinationResult = runCatching { destinationRepository.getDestination(destinationId) }
            destinationResult.fold(
                onSuccess = { destination ->
                    if (destination == null) {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = "Este destino ya no existe.")
                        }
                        return@fold
                    }
                    _uiState.update { it.copy(destination = destination) }

                    val packagesResult = runCatching {
                        packageRepository.getPublishedPackagesByDestination(destinationId)
                    }
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            packages = packagesResult.getOrElse { emptyList() }
                        )
                    }

                    if (destination.hasCoordinates) {
                        loadNearbyPlaces(destination, forceRefresh = false)
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "No se pudo cargar el destino.")
                    }
                }
            )
        }
    }

    fun refreshNearbyPlaces() {
        val destination = _uiState.value.destination ?: return
        if (destination.hasCoordinates) {
            loadNearbyPlaces(destination, forceRefresh = true)
        }
    }

    private fun loadNearbyPlaces(destination: TourDestination, forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(arePlacesLoading = true, placesErrorMessage = null) }
            val result = pointsOfInterestRepository.getNearbyPlaces(
                destinationId = destination.id,
                latitude = destination.latitude,
                longitude = destination.longitude,
                forceRefresh = forceRefresh
            )
            result.fold(
                onSuccess = { places ->
                    _uiState.update {
                        it.copy(
                            arePlacesLoading = false,
                            places = places,
                            arePlacesFromCache = !forceRefresh
                        )
                    }
                },
                onFailure = { error ->
                    val message = if (
                        error.message == PointsOfInterestRepository.ERROR_MISSING_GEOAPIFY_KEY
                    ) {
                        "El servicio de lugares cercanos no está configurado en esta compilación."
                    } else {
                        "No se pudieron cargar los lugares cercanos."
                    }
                    _uiState.update {
                        it.copy(
                            arePlacesLoading = false,
                            placesErrorMessage = message
                        )
                    }
                }
            )
        }
    }
}
