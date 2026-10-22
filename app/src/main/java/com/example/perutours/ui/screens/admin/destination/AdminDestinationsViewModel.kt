package com.example.perutours.ui.screens.admin.destination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.CatalogStatus
import com.example.perutours.data.model.TourDestination
import com.example.perutours.data.repository.DestinationCatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminDestinationsUiState(
    val isLoading: Boolean = true,
    val destinations: List<TourDestination> = emptyList(),
    val errorMessage: String? = null,
    val feedbackMessage: String? = null
)

/**
 * Lista administrativa de destinos: carga el catálogo completo (incluye
 * borradores y archivados) y gestiona el ciclo de vida PUBLICADO/ARCHIVADO.
 */
class AdminDestinationsViewModel(
    private val repository: DestinationCatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDestinationsUiState())
    val uiState: StateFlow<AdminDestinationsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = runCatching { repository.getAllDestinations() }
            result.fold(
                onSuccess = { destinations ->
                    _uiState.update {
                        it.copy(isLoading = false, destinations = destinations)
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No se pudo cargar el catálogo. Revisa tu conexión."
                        )
                    }
                }
            )
        }
    }

    fun changeStatus(destination: TourDestination, status: CatalogStatus) {
        viewModelScope.launch {
            _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
            val result = runCatching { repository.updateStatus(destination.id, status) }
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(feedbackMessage = feedbackFor(status, destination.name))
                    }
                    load()
                },
                onFailure = {
                    _uiState.update {
                        it.copy(errorMessage = "No se pudo actualizar el estado del destino.")
                    }
                }
            )
        }
    }

    private fun feedbackFor(status: CatalogStatus, name: String): String = when (status) {
        CatalogStatus.PUBLICADO -> "«$name» ya es visible para los clientes."
        CatalogStatus.ARCHIVADO -> "«$name» quedó archivado y dejó de mostrarse."
        CatalogStatus.BORRADOR -> "«$name» volvió a borrador."
    }
}
