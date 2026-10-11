package com.example.perutours.ui.screens.signup

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

    // Solo permite letras en español, tildes, ñ y espacios (máximo 60 caracteres)
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

    fun onRoleSelect(roleId: String) {
        _uiState.update { it.copy(selectedRoleId = roleId) }
    }

    fun dismissVerificationDialog() {
        _uiState.update { it.copy(showVerificationDialog = false) }
    }

    fun register() {
        val state = _uiState.value

        // Ejecutar validación pura a través de SignUpValidator
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

        // Si es válido, limpiar errores e invocar Firebase
        _uiState.update {
            it.copy(
                isLoading = true,
                generalError = null,
                nameError = null,
                emailError = null,
                phoneError = null,
                passwordError = null,
                confirmPasswordError = null
            )
        }

        val cleanEmail = state.email.trim()
        val cleanPass = state.password.trim()
        val cleanName = state.name.trim()
        val cleanPhone = state.phone.trim()

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
                            generalError = userFriendlyError
                        )
                    }
                }
            }
    }
}