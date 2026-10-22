package com.example.perutours.ui.screens.admin.packages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.CatalogStatus
import com.example.perutours.data.model.CatalogService
import com.example.perutours.data.model.TourDestination
import com.example.perutours.data.model.TourPackage
import com.example.perutours.data.repository.DestinationCatalogRepository
import com.example.perutours.data.repository.PackageRepository
import com.example.perutours.data.repository.ServiceCatalogRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminPackagesUiState(
    val isLoading: Boolean = true,
    val packages: List<TourPackage> = emptyList(),
    val destinations: List<TourDestination> = emptyList(),
    val catalogServices: List<CatalogService> = emptyList(),
    val isSaving: Boolean = false,
    val isEditorOpen: Boolean = false,
    val editingPackage: TourPackage? = null,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

/**
 * Gestión administrativa de paquetes turísticos.
 *
 * El administrador construye la oferta (destino, precio, duración, servicios
 * incluidos y condiciones), la guarda como borrador y la publica cuando está
 * completa. Solo los paquetes PUBLICADOS son visibles para el cliente.
 */
class AdminPackagesViewModel(
    private val packageRepository: PackageRepository,
    private val destinationRepository: DestinationCatalogRepository,
    private val serviceCatalogRepository: ServiceCatalogRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminPackagesUiState())
    val uiState: StateFlow<AdminPackagesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val packagesResult = runCatching { packageRepository.getAllPackages() }
            val destinationsResult = runCatching { destinationRepository.getAllDestinations() }
            val servicesResult = runCatching { serviceCatalogRepository.getServices() }

            val failure = packagesResult.exceptionOrNull()
                ?: destinationsResult.exceptionOrNull()
                ?: servicesResult.exceptionOrNull()

            if (failure != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No se pudo cargar el catálogo de paquetes."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        packages = packagesResult.getOrThrow().sortedByDescending { pkg -> pkg.updatedAtMillis },
                        destinations = destinationsResult.getOrThrow(),
                        catalogServices = servicesResult.getOrThrow()
                    )
                }
            }
        }
    }

    fun destinationNameOf(destinationId: String): String =
        _uiState.value.destinations.firstOrNull { it.id == destinationId }?.name.orEmpty()

    fun openEditor(tourPackage: TourPackage? = null) {
        _uiState.update {
            it.copy(isEditorOpen = true, editingPackage = tourPackage, errorMessage = null)
        }
    }

    fun closeEditor() {
        _uiState.update { it.copy(isEditorOpen = false, editingPackage = null) }
    }

    /** Persiste el paquete y su subcolección de servicios incluidos. */
    fun savePackage(tourPackage: TourPackage) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = runCatching {
                val uid = auth.currentUser?.uid.orEmpty()
                val toPersist = tourPackage.copy(
                    createdBy = tourPackage.createdBy.ifBlank { uid }
                )
                val saved = packageRepository.savePackage(toPersist)
                packageRepository.replaceIncludedServices(saved.id, saved.includedServiceIds)
                saved
            }
            result.fold(
                onSuccess = { saved ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isEditorOpen = false,
                            editingPackage = null,
                            feedbackMessage = "Paquete «${saved.title}» guardado."
                        )
                    }
                    load()
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "No se pudo guardar el paquete. Revisa los permisos y tu conexión."
                        )
                    }
                }
            )
        }
    }

    fun changeStatus(tourPackage: TourPackage, status: CatalogStatus) {
        viewModelScope.launch {
            _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
            val result = runCatching { packageRepository.updateStatus(tourPackage.id, status) }
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            feedbackMessage = when (status) {
                                CatalogStatus.PUBLICADO ->
                                    "«${tourPackage.title}» ya es visible para los clientes."
                                CatalogStatus.ARCHIVADO ->
                                    "«${tourPackage.title}» quedó archivado."
                                CatalogStatus.BORRADOR ->
                                    "«${tourPackage.title}» volvió a borrador."
                            }
                        )
                    }
                    load()
                },
                onFailure = {
                    _uiState.update {
                        it.copy(errorMessage = "No se pudo cambiar el estado del paquete.")
                    }
                }
            )
        }
    }
}
