package com.example.perutours.ui.screens.admin.service

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.CatalogService
import com.example.perutours.data.model.TourDestination
import com.example.perutours.data.model.TourProvider
import com.example.perutours.data.repository.DestinationCatalogRepository
import com.example.perutours.data.repository.ServiceCatalogRepository
import com.example.perutours.data.repository.TourProviderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminServicesUiState(
    val services: List<CatalogService> = emptyList(),
    val destinations: List<TourDestination> = emptyList(),
    val providers: List<TourProvider> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val editingService: CatalogService? = null,
    val isEditorOpen: Boolean = false,
    val message: String? = null
)

class AdminServicesViewModel(
    private val serviceRepository: ServiceCatalogRepository,
    private val destinationRepository: DestinationCatalogRepository,
    private val providerRepository: TourProviderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminServicesUiState())
    val uiState: StateFlow<AdminServicesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            runCatching {
                Triple(
                    serviceRepository.getServices(includeInactive = true),
                    destinationRepository.getAllDestinations(),
                    providerRepository.getProviders(includeInactive = true)
                )
            }.onSuccess { (services, destinations, providers) ->
                _uiState.update {
                    it.copy(
                        services = services,
                        destinations = destinations,
                        providers = providers,
                        isLoading = false
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(isLoading = false, message = "No pudimos cargar el catálogo de servicios.")
                }
            }
        }
    }

    fun openEditor(service: CatalogService? = null) {
        _uiState.update { it.copy(isEditorOpen = true, editingService = service, message = null) }
    }

    fun closeEditor() {
        _uiState.update { it.copy(isEditorOpen = false, editingService = null) }
    }

    fun saveService(
        name: String,
        destinationId: String,
        providerId: String,
        category: String,
        unitCostText: String,
        description: String,
        durationText: String,
        requiresScheduling: Boolean,
        active: Boolean,
        requestable: Boolean
    ) {
        val cost = unitCostText.replace(',', '.').toDoubleOrNull()
        val duration = durationText.toIntOrNull()
        ServiceFormValidator.validate(
            name,
            destinationId,
            providerId,
            category,
            cost,
            description,
            duration
        )?.let { error ->
            _uiState.update { it.copy(message = error) }
            return
        }

        val state = _uiState.value
        val destination = state.destinations.firstOrNull { it.id == destinationId }
        val provider = state.providers.firstOrNull { it.id == providerId }
        if (destination == null || provider == null) {
            _uiState.update { it.copy(message = "El destino o proveedor ya no está disponible.") }
            return
        }
        if (!provider.active && state.editingService?.providerId != provider.id) {
            _uiState.update { it.copy(message = "Activa el proveedor antes de asignarle servicios.") }
            return
        }

        val service = (state.editingService ?: CatalogService()).copy(
            name = name.trim(),
            destinationId = destination.id,
            providerId = provider.id,
            city = destination.name,
            category = category,
            unitCost = cost ?: 0.0,
            commercialCost = cost ?: 0.0,
            supplierName = provider.name,
            description = description.trim(),
            durationMinutes = duration ?: 60,
            requiresScheduling = requiresScheduling,
            active = active,
            requestable = requestable
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            runCatching { serviceRepository.saveService(service) }
                .onSuccess { saved ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isEditorOpen = false,
                            editingService = null,
                            message = "Servicio «${saved.name}» guardado."
                        )
                    }
                    load()
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isSaving = false, message = "No pudimos guardar el servicio.")
                    }
                }
        }
    }

    fun toggleActive(service: CatalogService) {
        viewModelScope.launch {
            runCatching { serviceRepository.setActive(service.id, !service.active) }
                .onSuccess { load() }
                .onFailure {
                    _uiState.update { it.copy(message = "No pudimos actualizar el servicio.") }
                }
        }
    }
}
