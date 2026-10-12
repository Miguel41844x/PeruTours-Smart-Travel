package com.example.perutours.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.messaging.FCMTokenManager
import com.example.perutours.data.repository.AuthRepository
import com.example.perutours.data.repository.LoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val fcmTokenManager: FCMTokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, infoMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, infoMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun resetPassword() {
        val email = _uiState.value.email
        val emailError = LoginValidator.validateEmailOnly(email)
        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError, infoMessage = emailError, isSuccessMessage = false) }
            return
        }

        viewModelScope.launch {
            authRepository.sendPasswordReset(email)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSuccessMessage = true,
                            infoMessage = "Te hemos enviado un enlace a $email para restablecer tu contraseña."
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSuccessMessage = false,
                            infoMessage = "Error: ${e.localizedMessage}"
                        )
                    }
                }
        }
    }

    fun login(onLoginSuccess: () -> Unit) {
        val state = _uiState.value
        val validation = LoginValidator.validate(state.email, state.password)
        if (!validation.isValid) {
            _uiState.update {
                it.copy(emailError = validation.emailError, passwordError = validation.passwordError)
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, infoMessage = null) }

        viewModelScope.launch {
            when (val result = authRepository.login(state.email, state.password)) {
                is LoginResult.Success -> {
                    runCatching { fcmTokenManager.saveToken() }
                    _uiState.update { it.copy(isLoading = false) }
                    onLoginSuccess()
                }
                is LoginResult.EmailNotVerified -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccessMessage = false,
                            infoMessage = "Tu correo aún no está verificado. Te enviamos un nuevo enlace a tu bandeja de entrada."
                        )
                    }
                }
                is LoginResult.Error -> {
                    val friendly = when {
                        result.message.contains("invalid-credential", true) -> "Correo o contraseña incorrectos."
                        result.message.contains("badly formatted", true) -> "El formato de correo no es válido."
                        else -> "Error: ${result.message}"
                    }
                    _uiState.update {
                        it.copy(isLoading = false, isSuccessMessage = false, infoMessage = friendly)
                    }
                }
            }
        }
    }
}