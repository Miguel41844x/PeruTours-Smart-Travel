package com.example.perutours.ui.screens.signup

import android.util.Patterns
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SignUpViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, errorMessage = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPhoneChange(phone: String) {
        if (phone.length <= 9 && phone.all { it.isDigit() }) {
            _uiState.update { it.copy(phone = phone, errorMessage = null) }
        }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
    }

    fun onRoleSelect(roleId: String) {
        _uiState.update { it.copy(selectedRoleId = roleId) }
    }

    fun dismissVerificationDialog() {
        _uiState.update { it.copy(showVerificationDialog = false) }
    }

    fun register() {
        val state = _uiState.value
        val cleanName = state.name.trim()
        val cleanEmail = state.email.trim()
        val cleanPhone = state.phone.trim()
        val cleanPass = state.password.trim()
        val cleanConfirm = state.confirmPassword.trim()

        // Validaciones estrictas
        when {
            cleanName.isBlank() || cleanEmail.isBlank() || cleanPhone.isBlank() || cleanPass.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Por favor completa todos los campos obligatorios.") }
            }
            !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> {
                _uiState.update { it.copy(errorMessage = "El formato del correo electrónico no es válido.") }
            }
            cleanPhone.length != 9 || !cleanPhone.startsWith("9") -> {
                _uiState.update { it.copy(errorMessage = "Ingresa un celular válido de 9 dígitos que comience con 9.") }
            }
            cleanPass.length < 6 -> {
                _uiState.update { it.copy(errorMessage = "La contraseña debe tener al menos 6 caracteres.") }
            }
            !cleanPass.any { it.isUpperCase() } -> {
                _uiState.update { it.copy(errorMessage = "La contraseña debe contener al menos una letra mayúscula.") }
            }
            !cleanPass.any { it.isDigit() } -> {
                _uiState.update { it.copy(errorMessage = "La contraseña debe contener al menos un número.") }
            }
            !cleanPass.any { !it.isLetterOrDigit() } -> {
                _uiState.update { it.copy(errorMessage = "La contraseña debe contener al menos un símbolo (ej. @ # $ % & *).") }
            }
            cleanPass != cleanConfirm -> {
                _uiState.update { it.copy(errorMessage = "Las contraseñas no coinciden.") }
            }
            else -> {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                auth.createUserWithEmailAndPassword(cleanEmail, cleanPass)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val profileUpdates = userProfileChangeRequest {
                                displayName = "$cleanName | ${state.selectedRoleId} | $cleanPhone"
                            }

                            user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                                user.sendEmailVerification().addOnCompleteListener {
                                    auth.signOut()
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            showVerificationDialog = true
                                        )
                                    }
                                }
                            }
                        } else {
                            val errorMsg = task.exception?.localizedMessage.orEmpty()
                            val userFriendlyError = when {
                                errorMsg.contains("already in use", ignoreCase = true) ->
                                    "Este correo electrónico ya está registrado."
                                else -> "Error al registrarse: $errorMsg"
                            }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = userFriendlyError
                                )
                            }
                        }
                    }
            }
        }
    }
}