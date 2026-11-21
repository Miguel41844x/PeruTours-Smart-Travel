package com.example.perutours.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.messaging.FCMTokenManager
import com.example.perutours.data.model.UserRole
import com.example.perutours.data.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoleRouterUiState(
    val isLoading: Boolean = true,
    val destinationRoute: String? = null,
    val errorMessage: String? = null
)

class RoleRouterViewModel(
    private val auth: FirebaseAuth,
    private val profileRepository: ProfileRepository,
    private val fcmTokenManager: FCMTokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoleRouterUiState())
    val uiState: StateFlow<RoleRouterUiState> = _uiState.asStateFlow()

    init {
        routeCurrentUser()
    }

    fun routeCurrentUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val currentUser = auth.currentUser
            if (currentUser == null) {
                _uiState.value = RoleRouterUiState(isLoading = false, destinationRoute = "initial")
                return@launch
            }
            launch { runCatching { fcmTokenManager.saveToken() } }
            runCatching { profileRepository.getProfile(currentUser.uid) }
                .onSuccess { profile ->
                    val role = profile.userRole
                    if (role == UserRole.UNKNOWN) {
                        _uiState.value = RoleRouterUiState(
                            isLoading = false,
                            errorMessage = "Tu cuenta no tiene un rol válido. Solicita ayuda al administrador."
                        )
                    } else {
                        _uiState.value = RoleRouterUiState(
                            isLoading = false,
                            destinationRoute = role.startRoute
                        )
                    }
                }
                .onFailure {
                    _uiState.value = RoleRouterUiState(
                        isLoading = false,
                        errorMessage = "No pudimos abrir tu cuenta. Revisa tu conexión e inténtalo nuevamente."
                    )
                }
        }
    }
}
