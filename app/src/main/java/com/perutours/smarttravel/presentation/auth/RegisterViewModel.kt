package com.perutours.smarttravel.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perutours.smarttravel.di.AppModule
import com.perutours.smarttravel.domain.usecase.RegisterUseCase
import com.perutours.smarttravel.domain.usecase.SendEmailVerificationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    private val registerUseCase: RegisterUseCase = AppModule.registerUseCase
    private val sendEmailVerificationUseCase: SendEmailVerificationUseCase = AppModule.sendEmailVerificationUseCase

    private val _formState = MutableStateFlow(RegisterFormState())
    val formState: StateFlow<RegisterFormState> = _formState.asStateFlow()

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onFullNameChanged(value: String) {
        _formState.update { it.copy(fullName = RegisterFieldState(value = value, error = null)) }
    }

    fun onEmailChanged(value: String) {
        _formState.update { it.copy(email = RegisterFieldState(value = value, error = null)) }
    }

    fun onPhoneChanged(value: String) {
        _formState.update { it.copy(phone = RegisterFieldState(value = value, error = null)) }
    }

    fun onPasswordChanged(value: String) {
        _formState.update { it.copy(password = RegisterFieldState(value = value, error = null)) }
    }

    fun togglePasswordVisibility() {
        _formState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onRoleSelected(role: TouristRole) {
        _formState.update { it.copy(selectedRole = role) }
    }

    fun onPreferenceToggled(preference: TravelPreference) {
        _formState.update { state ->
            val newPrefs = if (preference in state.selectedPreferences) {
                state.selectedPreferences - preference
            } else {
                state.selectedPreferences + preference
            }
            state.copy(selectedPreferences = newPrefs)
        }
    }

    fun nextStep() {
        val current = _formState.value.currentStep
        val nextOrdinal = current.ordinal + 1
        if (nextOrdinal < RegisterStep.entries.size) {
            _formState.update { it.copy(currentStep = RegisterStep.entries[nextOrdinal]) }
        }
    }

    fun previousStep() {
        val current = _formState.value.currentStep
        val prevOrdinal = current.ordinal - 1
        if (prevOrdinal >= 0) {
            _formState.update { it.copy(currentStep = RegisterStep.entries[prevOrdinal]) }
        }
    }

    fun isStepValid(): Boolean {
        val form = _formState.value
        return when (form.currentStep) {
            RegisterStep.PERSONAL_DATA -> {
                form.fullName.value.isNotBlank() &&
                        form.email.value.isNotBlank() &&
                        form.password.value.isNotBlank()
            }
            RegisterStep.ROLE -> form.selectedRole != null
            RegisterStep.PREFERENCES -> form.selectedPreferences.isNotEmpty()
        }
    }

    fun validateStep1(): Boolean {
        val form = _formState.value
        var hasError = false

        if (form.fullName.value.isBlank()) {
            _formState.update { it.copy(fullName = it.fullName.copy(error = "El nombre es obligatorio")) }
            hasError = true
        }

        if (form.email.value.isBlank()) {
            _formState.update { it.copy(email = it.email.copy(error = "El email es obligatorio")) }
            hasError = true
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(form.email.value).matches()) {
            _formState.update { it.copy(email = it.email.copy(error = "Formato de email inválido")) }
            hasError = true
        }

        if (form.password.value.isBlank()) {
            _formState.update { it.copy(password = it.password.copy(error = "La contraseña es obligatoria")) }
            hasError = true
        } else if (form.password.value.length < 4) {
            _formState.update { it.copy(password = it.password.copy(error = "Mínimo 4 caracteres")) }
            hasError = true
        }

        return !hasError
    }

    fun register() {
        val form = _formState.value
        val nameParts = form.fullName.value.trim().split("\\s+".toRegex(), limit = 2)
        val firstName = nameParts.getOrElse(0) { "" }
        val lastName = nameParts.getOrElse(1) { "" }

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            val result = registerUseCase.execute(
                email = form.email.value,
                password = form.password.value,
                firstName = firstName,
                lastName = lastName
            )
            result.fold(
                onSuccess = {
                    sendEmailVerificationUseCase()
                    _uiState.value = RegisterUiState.Success(
                        "Cuenta creada. Revisa ${form.email.value} para verificar tu correo."
                    )
                },
                onFailure = { error ->
                    _uiState.value = RegisterUiState.Error(
                        error.message ?: "Error al registrar la cuenta"
                    )
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = RegisterUiState.Idle
    }

    fun clearForm() {
        _formState.value = RegisterFormState()
        _uiState.value = RegisterUiState.Idle
    }
}
