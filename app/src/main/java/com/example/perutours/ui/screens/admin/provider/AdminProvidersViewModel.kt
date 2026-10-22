package com.example.perutours.ui.screens.admin.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.TourProvider
import com.example.perutours.data.repository.TourProviderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminProvidersUiState(
    val isLoading: Boolean = true,
    val providers: List<TourProvider> = emptyList(),
    val errorMessage: String? = null,
    val feedbackMessage: String? = null,
    val isEditorOpen: Boolean = false,
    val isSaving: Boolean = false,
    val editingProvider: TourProvider? = null
)

/**
 * Gestión administrativa de proveedores: alta, edición y activación.
 * Un proveedor desactivado deja de estar disponible para nuevos paquetes.
 */
class AdminProvidersViewModel(
    private val repository: TourProviderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminProvidersUiState())
    val uiState: StateFlow<AdminProvidersUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = runCatching { repository.getProviders(includeInactive = true) }
            result.fold(
                onSuccess = { providers ->
                    _uiState.update { it.copy(isLoading = false, providers = providers) }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No se pudieron cargar los proveedores."
                        )
                    }
                }
            )
        }
    }

    fun openEditor(provider: TourProvider? = null) {
        _uiState.update {
            it.copy(isEditorOpen = true, editingProvider = provider, errorMessage = null)
        }
    }

    fun closeEditor() {
        _uiState.update { it.copy(isEditorOpen = false, editingProvider = null) }
    }

    fun saveProvider(
        name: String,
        contactName: String,
        phone: String,
        email: String,
        city: String,
        servicesText: String,
        active: Boolean
    ) {
        val nameError = ProviderFormValidator.validateName(name)
        val cityError = ProviderFormValidator.validateCity(city)
        val emailError = ProviderFormValidator.validateEmail(email)
        val phoneError = ProviderFormValidator.validatePhone(phone)

        if (nameError != null || cityError != null || emailError != null || phoneError != null) {
            _uiState.update {
                it.copy(errorMessage = listOfNotNull(nameError, cityError, emailError, phoneError).first())
            }
            return
        }

        val provider = ProviderFormValidator.buildProvider(
            existing = _uiState.value.editingProvider,
            name = name,
            contactName = contactName,
            phone = phone,
            email = email,
            city = city,
            servicesText = servicesText,
            active = active,
            nowMillis = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = runCatching { repository.saveProvider(provider) }
            result.fold(
                onSuccess = { saved ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isEditorOpen = false,
                            editingProvider = null,
                            feedbackMessage = "Proveedor «${saved.name}» guardado."
                        )
                    }
                    load()
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "No se pudo guardar el proveedor."
                        )
                    }
                }
            )
        }
    }

    fun toggleActive(provider: TourProvider) {
        viewModelScope.launch {
            val result = runCatching { repository.setActive(provider.id, !provider.active) }
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            feedbackMessage = if (!provider.active) {
                                "«${provider.name}» activado."
                            } else {
                                "«${provider.name}» desactivado."
                            }
                        )
                    }
                    load()
                },
                onFailure = {
                    _uiState.update {
                        it.copy(errorMessage = "No se pudo actualizar el proveedor.")
                    }
                }
            )
        }
    }
}
