package com.example.perutours.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.repository.DestinationRepository
import com.example.perutours.data.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application,
    private val repository: DestinationRepository,
    private val profileRepository: ProfileRepository,
    private val auth: FirebaseAuth
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // 1. Cargar datos iniciales del usuario desde FirebaseAuth y observar cambios en Firestore
        observeUserProfile()

        // 2. Escuchar Room SQLite en tiempo real (Single Source of Truth)
        observeLocalDatabase()

        // 3. Sincronizar con la API remota respetando la política de expiración
        fetchDestinations(forceRefresh = false)
    }

    private fun observeUserProfile() {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid.orEmpty()
        val rawName = currentUser?.displayName ?: "viajero"
        val initialName = rawName.substringBefore("|").trim()
        val initialPhoto = currentUser?.photoUrl?.toString().orEmpty()

        _uiState.update {
            it.copy(
                userName = initialName,
                photoUrl = initialPhoto
            )
        }

        if (uid.isNotBlank()) {
            viewModelScope.launch {
                val availablePreferences = try {
                    profileRepository.getPreferences()
                } catch (_: Exception) {
                    emptyList()
                }
                val prefMap = availablePreferences.associate { it.id to it.name }

                profileRepository.observeUserProfile(uid).collect { userProfile ->
                    val names = userProfile.preferences.mapNotNull { prefId ->
                        prefMap[prefId]?.takeIf { it.isNotBlank() }
                    }
                    _uiState.update { state ->
                        state.copy(
                            userName = userProfile.name.ifBlank { initialName },
                            photoUrl = userProfile.photoUrl.ifBlank { initialPhoto },
                            preferenceNames = names
                        )
                    }
                }
            }
        }
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
