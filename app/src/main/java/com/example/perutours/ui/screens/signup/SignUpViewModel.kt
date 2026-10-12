package com.example.perutours.ui.screens.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignUpViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        val isOnlyLettersAndSpaces = name.all { it.isLetter() || it.isWhitespace() }
        if (isOnlyLettersAndSpaces && name.length <= 60) {
            _uiState.update { it.copy(name = name, nameError = null, generalError = null) }
        }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, generalError = null) }
    }

    fun onPhoneChange(phone: String) {
        if (phone.length <= 9 && phone.all { it.isDigit() }) {
            _uiState.update { it.copy(phone = phone, phoneError = null, generalError = null) }
        }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, generalError = null) }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null, generalError = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
    }

    fun dismissVerificationDialog() {
        _uiState.update { it.copy(showVerificationDialog = false) }
    }

    fun register() {
        val state = _uiState.value

        val validation = SignUpValidator.validate(
            name = state.name,
            email = state.email,
            phone = state.phone,
            password = state.password,
            confirmPassword = state.confirmPassword
        )

        if (!validation.isValid) {
            _uiState.update {
                it.copy(
                    nameError = validation.nameError,
                    emailError = validation.emailError,
                    phoneError = validation.phoneError,
                    passwordError = validation.passwordError,
                    confirmPasswordError = validation.confirmPasswordError
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, generalError = null) }

        viewModelScope.launch {
            val result = authRepository.registerClient(
                name = state.name,
                email = state.email,
                password = state.password,
                phone = state.phone
            )

            result.onSuccess {
                _uiState.update {
                    it.copy(isLoading = false, showVerificationDialog = true)
                }
            }.onFailure { error ->
                val errorMsg = error.localizedMessage.orEmpty()
                val friendly = when {
                    errorMsg.contains("already in use", true) -> "Este correo electrónico ya está registrado."
                    else -> "Error al registrarse: $errorMsg"
                }
                _uiState.update { it.copy(isLoading = false, generalError = friendly) }
            }
        }
    }
}