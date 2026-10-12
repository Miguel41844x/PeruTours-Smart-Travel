package com.example.perutours.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.local.AppDatabase
import com.example.perutours.data.remote.NetworkClient
import com.example.perutours.data.repository.DestinationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application,
    private val repository: DestinationRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // 1. Escuchar Room SQLite en tiempo real (Single Source of Truth)
        observeLocalDatabase()

        // 2. Sincronizar con la API remota respetando la política de expiración
        fetchDestinations(forceRefresh = false)
    }

    private fun observeLocalDatabase() {
        viewModelScope.launch {
            repository.localDestinations.collect { entities ->
                if (entities.isNotEmpty()) {
                    val dtos = entities.map { it.toDto() }
                    val fromCache = repository.isCacheExpired()
                    _uiState.update {
                        it.copy(
                            destinationsState = HomeDestinationsState.Success(
                                destinations = dtos,
                                fromCache = fromCache
                            )
                        )
                    }
                }
            }
        }
    }

    fun fetchDestinations(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            if (_uiState.value.destinationsState !is HomeDestinationsState.Success) {
                _uiState.update { it.copy(destinationsState = HomeDestinationsState.Loading) }
            }

            val result = repository.refreshDestinations(forceRefresh = forceRefresh)
            result.onFailure {
                if (_uiState.value.destinationsState !is HomeDestinationsState.Success) {
                    _uiState.update {
                        it.copy(
                            destinationsState = HomeDestinationsState.Error(
                                "Revisa tu conexión e inténtalo nuevamente."
                            )
                        )
                    }
                }
            }
        }
    }
}