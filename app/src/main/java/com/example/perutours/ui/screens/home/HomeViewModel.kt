package com.example.perutours.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.remote.NetworkClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val apiService = NetworkClient.getApiService(application.applicationContext)
    private val auth = FirebaseAuth.getInstance()

    init {
        loadUserProfile()
        fetchDestinations()
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        val displayName = user?.displayName?.split("|")?.firstOrNull()?.trim()
        val finalName = if (!displayName.isNullOrBlank()) displayName else "Viajero"
        _uiState.update { it.copy(userName = finalName) }
    }

    // Criterio 1 y 2: Consumo de API externa, cache y emisión reactiva con Flow
    fun fetchDestinations() {
        viewModelScope.launch {
            _uiState.update { it.copy(destinationsState = HomeDestinationsState.Loading) }
            try {
                val destinations = apiService.getDestinations()
                _uiState.update {
                    it.copy(destinationsState = HomeDestinationsState.Success(destinations))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(destinationsState = HomeDestinationsState.Error(
                        e.localizedMessage ?: "Error al conectar con el servidor de destinos"
                    ))
                }
            }
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
}