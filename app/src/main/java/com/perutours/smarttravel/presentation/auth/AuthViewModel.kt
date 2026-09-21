package com.perutours.smarttravel.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perutours.smarttravel.di.AppModule
import com.perutours.smarttravel.domain.usecase.LoginUseCase
import com.perutours.smarttravel.domain.usecase.LogoutUseCase
import com.perutours.smarttravel.domain.usecase.ObserveAuthStateUseCase
import com.perutours.smarttravel.domain.usecase.ReloadUserUseCase
import com.perutours.smarttravel.domain.usecase.SendEmailVerificationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val loginUseCase: LoginUseCase = AppModule.loginUseCase
    private val observeAuthStateUseCase: ObserveAuthStateUseCase = AppModule.observeAuthStateUseCase
    private val logoutUseCase: LogoutUseCase = AppModule.logoutUseCase
    private val reloadUserUseCase: ReloadUserUseCase = AppModule.reloadUserUseCase
    private val sendEmailVerificationUseCase: SendEmailVerificationUseCase = AppModule.sendEmailVerificationUseCase

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { user ->
                if (user != null) {
                    if (user.isEmailVerified) {
                        _uiState.value = AuthUiState.Authenticated(user)
                    } else {
                        _uiState.value = AuthUiState.EmailNotVerified(user)
                    }
                } else {
                    val current = _uiState.value
                    if (current is AuthUiState.Authenticated || current is AuthUiState.EmailNotVerified) {
                        _uiState.value = AuthUiState.Unauthenticated
                    }
                }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = loginUseCase(email, password)
            result.fold(
                onSuccess = { user ->
                    if (user.isEmailVerified) {
                        _uiState.value = AuthUiState.Authenticated(user)
                    } else {
                        _uiState.value = AuthUiState.EmailNotVerified(user)
                    }
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(
                        error.message ?: "Error desconocido al iniciar sesión"
                    )
                }
            )
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            sendEmailVerificationUseCase()
        }
    }

    fun checkEmailVerification() {
        viewModelScope.launch {
            val user = reloadUserUseCase()
            if (user != null) {
                if (user.isEmailVerified) {
                    _uiState.value = AuthUiState.Authenticated(user)
                } else {
                    _uiState.value = AuthUiState.EmailNotVerified(user)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _uiState.value = AuthUiState.Unauthenticated
        }
    }

    fun clearError() {
        _uiState.update { state ->
            if (state is AuthUiState.Error) AuthUiState.Idle else state
        }
    }
}
