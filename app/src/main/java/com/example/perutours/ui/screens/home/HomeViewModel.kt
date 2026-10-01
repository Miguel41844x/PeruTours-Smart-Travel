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

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DestinationRepository

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getInstance(application)
        val apiService = NetworkClient.getApiService(application)
        repository = DestinationRepository(apiService, database.destinationDao())

        // 1. Escuchar Room SQLite en tiempo real (Single Source of Truth)
        observeLocalDatabase()

        // 2. Sincronizar con la API remota
        fetchDestinations()
    }

    private fun observeLocalDatabase() {
        viewModelScope.launch {
            repository.localDestinations.collect { entities ->
                if (entities.isNotEmpty()) {
                    val dtos = entities.map { it.toDto() }
                    _uiState.update {
                        it.copy(destinationsState = HomeDestinationsState.Success(dtos))
                    }
                }
            }
        }
    }

    fun fetchDestinations() {
        viewModelScope.launch {
            // Si la base de datos aún no tiene nada en caché, mostramos Loading
            if (_uiState.value.destinationsState !is HomeDestinationsState.Success) {
                _uiState.update { it.copy(destinationsState = HomeDestinationsState.Loading) }
            }

            val result = repository.refreshDestinations()

            result.onFailure { error ->
                // Si la BD local no tiene datos y falló el internet, mostramos Error
                if (_uiState.value.destinationsState !is HomeDestinationsState.Success) {
                    _uiState.update {
                        it.copy(
                            destinationsState = HomeDestinationsState.Error(
                                error.localizedMessage ?: "No se pudo conectar al servidor"
                            )
                        )
                    }
                }
            }
        }
    }
}