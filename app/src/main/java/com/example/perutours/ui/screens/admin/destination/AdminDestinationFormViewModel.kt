package com.example.perutours.ui.screens.admin.destination

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.CatalogStatus
import com.example.perutours.data.model.TourDestination
import com.example.perutours.data.repository.CatalogImageRepository
import com.example.perutours.data.repository.DestinationCatalogRepository
import com.example.perutours.data.repository.GeocodingRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminDestinationFormUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingImage: Boolean = false,
    val isResolvingCoordinates: Boolean = false,
    val destinationId: String = "",
    val name: String = "",
    val department: String = "",
    val summary: String = "",
    val latitudeText: String = "",
    val longitudeText: String = "",
    val coverImageUrl: String = "",
    val categoriesText: String = "",
    val externalPlaceId: String = "",
    val status: CatalogStatus = CatalogStatus.BORRADOR,
    val createdBy: String = "",
    val createdAtMillis: Long = 0L,
    val nameError: String? = null,
    val departmentError: String? = null,
    val coordinatesError: String? = null,
    val imageUrlError: String? = null,
    val errorMessage: String? = null,
    val savedDestinationId: String? = null
) {
    val isEditing: Boolean get() = destinationId.isNotBlank()
}

/**
 * Formulario de creación/edición de destinos.
 *
 * Proceso del catálogo: registrar el destino, guardar sus coordenadas
 * (validadas con geocodificación) y decidir su estado de publicación.
 */
class AdminDestinationFormViewModel(
    private val repository: DestinationCatalogRepository,
    private val imageRepository: CatalogImageRepository,
    private val geocodingRepository: GeocodingRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDestinationFormUiState())
    val uiState: StateFlow<AdminDestinationFormUiState> = _uiState.asStateFlow()

    private var existingDestination: TourDestination? = null

    fun load(destinationId: String) {
        if (destinationId.isBlank()) return
        if (_uiState.value.destinationId == destinationId && existingDestination != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = runCatching { repository.getDestination(destinationId) }
            result.fold(
                onSuccess = { destination ->
                    if (destination == null) {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = "El destino ya no existe.")
                        }
                    } else {
                        existingDestination = destination
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                destinationId = destination.id,
                                name = destination.name,
                                department = destination.department,
                                summary = destination.summary,
                                latitudeText = destination.latitude.takeIf { lat -> lat != 0.0 }
                                    ?.toString().orEmpty(),
                                longitudeText = destination.longitude.takeIf { lng -> lng != 0.0 }
                                    ?.toString().orEmpty(),
                                coverImageUrl = destination.coverImageUrl,
                                categoriesText = destination.categories.joinToString(", "),
                                externalPlaceId = destination.externalPlaceId,
                                status = CatalogStatus.from(destination.status),
                                createdBy = destination.createdBy,
                                createdAtMillis = destination.createdAtMillis
                            )
                        }
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "No se pudo abrir el destino.")
                    }
                }
            )
        }
    }

    fun onNameChange(value: String) = _uiState.update {
        val locationChanged = value.trim() != it.name.trim()
        it.copy(
            name = value,
            nameError = null,
            latitudeText = if (locationChanged) "" else it.latitudeText,
            longitudeText = if (locationChanged) "" else it.longitudeText,
            externalPlaceId = if (locationChanged) "" else it.externalPlaceId,
            savedDestinationId = null
        )
    }

    fun onDepartmentChange(value: String) = _uiState.update {
        val locationChanged = value.trim() != it.department.trim()
        it.copy(
            department = value,
            departmentError = null,
            latitudeText = if (locationChanged) "" else it.latitudeText,
            longitudeText = if (locationChanged) "" else it.longitudeText,
            externalPlaceId = if (locationChanged) "" else it.externalPlaceId,
            savedDestinationId = null
        )
    }

    fun onSummaryChange(value: String) = _uiState.update {
        it.copy(summary = value, savedDestinationId = null)
    }

    fun onLatitudeChange(value: String) = _uiState.update {
        it.copy(latitudeText = value, coordinatesError = null, savedDestinationId = null)
    }

    fun onLongitudeChange(value: String) = _uiState.update {
        it.copy(longitudeText = value, coordinatesError = null, savedDestinationId = null)
    }

    fun onImageUrlChange(value: String) = _uiState.update {
        it.copy(coverImageUrl = value, imageUrlError = null, savedDestinationId = null)
    }

    fun onCategoriesChange(value: String) = _uiState.update {
        it.copy(categoriesText = value, savedDestinationId = null)
    }

    fun onExternalPlaceIdChange(value: String) = _uiState.update {
        it.copy(externalPlaceId = value, savedDestinationId = null)
    }

    /** Guarda el destino validando los campos antes de escribir en Firestore. */
    fun save(shouldPublish: Boolean = false) {
        val destination = buildValidatedDestination(shouldPublish) ?: return
        viewModelScope.launch { persist(destination) }
    }

    /**
     * Sube la imagen elegida por el administrador a Firebase Storage.
     * Si el destino aún no existe se guarda primero un borrador, porque la
     * ruta destination_images/{destinationId}/... necesita el ID real.
     */
    fun onCoverImagePicked(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingImage = true, errorMessage = null) }

            var destinationId = _uiState.value.destinationId
            if (destinationId.isBlank()) {
                val draft = buildValidatedDestination(shouldPublish = false)
                if (draft == null) {
                    _uiState.update { it.copy(isUploadingImage = false) }
                    return@launch
                }
                destinationId = persist(draft)?.id.orEmpty()
            }

            if (destinationId.isBlank()) {
                _uiState.update { it.copy(isUploadingImage = false) }
                return@launch
            }

            val result = runCatching {
                imageRepository.uploadDestinationCover(destinationId, imageUri)
            }
            result.fold(
                onSuccess = { url ->
                    _uiState.update {
                        it.copy(isUploadingImage = false, coverImageUrl = url)
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isUploadingImage = false,
                            errorMessage = "No se pudo subir la imagen (máximo 5 MB, JPG/PNG/WebP)."
                        )
                    }
                }
            )
        }
    }

    /** Valida el formulario y construye el modelo a persistir. */
    private fun buildValidatedDestination(shouldPublish: Boolean): TourDestination? {
        val state = _uiState.value
        val nameError = DestinationFormValidator.validateName(state.name)
        val departmentError = DestinationFormValidator.validateDepartment(state.department)
        val summaryError = DestinationFormValidator.validateSummary(state.summary)
        val imageUrlError = DestinationFormValidator.validateImageUrl(state.coverImageUrl)
        val coordinatesError = if (shouldPublish || state.status == CatalogStatus.PUBLICADO) {
            DestinationFormValidator.validateForPublish(
                state.latitudeText,
                state.longitudeText,
                state.externalPlaceId
            )
        } else {
            DestinationFormValidator.validateCoordinates(state.latitudeText, state.longitudeText)
        }

        if (nameError != null || departmentError != null || summaryError != null ||
            imageUrlError != null || coordinatesError != null
        ) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    departmentError = departmentError,
                    coordinatesError = coordinatesError,
                    imageUrlError = imageUrlError,
                    errorMessage = summaryError
                )
            }
            return null
        }

        val targetStatus = if (shouldPublish) CatalogStatus.PUBLICADO else state.status
        return DestinationFormValidator.buildDestination(
            existing = existingDestination,
            name = state.name,
            department = state.department,
            summary = state.summary,
            latitudeText = state.latitudeText,
            longitudeText = state.longitudeText,
            coverImageUrl = state.coverImageUrl,
            categoriesText = state.categoriesText,
            externalPlaceId = state.externalPlaceId,
            status = targetStatus,
            createdBy = state.createdBy.ifBlank { auth.currentUser?.uid.orEmpty() },
            nowMillis = System.currentTimeMillis()
        )
    }

    private suspend fun persist(destination: TourDestination): TourDestination? {
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        val result = runCatching { repository.saveDestination(destination) }
        return result.fold(
            onSuccess = { saved ->
                existingDestination = saved
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        destinationId = saved.id,
                        status = CatalogStatus.from(saved.status),
                        createdAtMillis = saved.createdAtMillis,
                        savedDestinationId = saved.id
                    )
                }
                saved
            },
            onFailure = {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "No se pudo guardar. Revisa los permisos y tu conexión."
                    )
                }
                null
            }
        )
    }

    /** Fija coordenadas ya resueltas por geocodificación externa. */
    fun applyCoordinates(latitude: Double, longitude: Double, placeId: String?) {
        _uiState.update {
            it.copy(
                latitudeText = latitude.toString(),
                longitudeText = longitude.toString(),
                externalPlaceId = placeId?.takeIf { id -> id.isNotBlank() } ?: it.externalPlaceId,
                coordinatesError = null,
                savedDestinationId = null
            )
        }
    }

    /**
     * Valida el nombre del destino contra la geocodificación externa
     * (Geoapify) y fija las coordenadas reales del lugar. Publicar un
     * destino exige este paso para poder consultar después los puntos de
     * interés cercanos.
     */
    fun resolveCoordinates() {
        val state = _uiState.value
        val nameError = DestinationFormValidator.validateName(state.name)
        val departmentError = DestinationFormValidator.validateDepartment(state.department)
        if (nameError != null || departmentError != null) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    departmentError = departmentError,
                    errorMessage = "Completa el nombre y el departamento antes de validar."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isResolvingCoordinates = true, errorMessage = null) }
            val result = geocodingRepository.geocodeDestination(state.name, state.department)
            result.fold(
                onSuccess = { geocoded ->
                    applyCoordinates(geocoded.latitude, geocoded.longitude, geocoded.placeId)
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            coordinatesError = "Geoapify no encontró «${state.name}». " +
                                "Revisa el nombre o ajusta las coordenadas manualmente."
                        )
                    }
                }
            )
            _uiState.update { it.copy(isResolvingCoordinates = false) }
        }
    }
}
