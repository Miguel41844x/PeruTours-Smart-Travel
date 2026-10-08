package com.example.perutours.ui.screens.login

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.perutours.data.messaging.FCMTokenManager
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

class LoginViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val fcmTokenManager = FCMTokenManager()

    fun onEmailChange(email: String) {
        _uiState.update {
            it.copy(email = email, emailError = null, infoMessage = null)
        }
    }

    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(password = password, passwordError = null, infoMessage = null)
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update {
            it.copy(passwordVisible = !it.passwordVisible)
        }
    }

    // Criterio 4: Recuperación de contraseña por correo
    fun resetPassword() {
        val email = _uiState.value.email
        val emailError = LoginValidator.validateEmailOnly(email)

        if (emailError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    infoMessage = emailError,
                    isSuccessMessage = false
                )
            }
            return
        }

        val cleanEmail = email.trim()
        auth.sendPasswordResetEmail(cleanEmail)
            .addOnSuccessListener {
                _uiState.update {
                    it.copy(
                        isSuccessMessage = true,
                        infoMessage = "Te hemos enviado un enlace a $cleanEmail para restablecer tu contraseña."
                    )
                }
            }
            .addOnFailureListener { e ->
                _uiState.update {
                    it.copy(
                        isSuccessMessage = false,
                        infoMessage = "Error: ${e.localizedMessage}"
                    )
                }
            }
    }

    // Criterio 1 y Criterio 2: Login con verificación estricta de correo
    fun login(onLoginSuccess: () -> Unit) {
        val state = _uiState.value

        val validation = LoginValidator.validate(state.email, state.password)
        if (!validation.isValid) {
            _uiState.update {
                it.copy(
                    emailError = validation.emailError,
                    passwordError = validation.passwordError,
                    infoMessage = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                infoMessage = null,
                emailError = null,
                passwordError = null
            )
        }

        val cleanEmail = state.email.trim()
        val cleanPass = state.password.trim()

        auth.signInWithEmailAndPassword(cleanEmail, cleanPass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    // Criterio 2: Bloquear acceso si no ha verificado su email
                    if (user != null && user.isEmailVerified) {

                        viewModelScope.launch {

                            try {
                                fcmTokenManager.saveToken()
                            } catch (e: Exception) {
                                // No impedimos el acceso si falla FCM.
                            }

                            _uiState.update {
                                it.copy(isLoading = false)
                            }

                            onLoginSuccess()
                        }
                    } else if (user != null) {
                        user.sendEmailVerification()
                            .addOnCompleteListener { verificationTask ->
                                auth.signOut()
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isSuccessMessage = verificationTask.isSuccessful,
                                        infoMessage = if (verificationTask.isSuccessful) {
                                            "Tu correo aún no está verificado. Te enviamos un nuevo enlace a tu bandeja de entrada."
                                        } else {
                                            "Tu correo aún no está verificado y no pudimos reenviar el enlace."
                                        }
                                    )
                                }
                            }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSuccessMessage = false,
                                infoMessage = "No se pudo recuperar la sesión. Inténtalo nuevamente."
                            )
                        }
                    }
                } else {
                    val errorMsg = task.exception?.localizedMessage.orEmpty()
                    val friendlyError = when {
                        errorMsg.contains("badly formatted", true) -> "El formato de correo no es válido."
                        errorMsg.contains("invalid-credential", true) -> "Correo o contraseña incorrectos."
                        else -> "Error al iniciar sesión. Revisa tus credenciales."
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccessMessage = false,
                            infoMessage = friendlyError
                        )
                    }
                }
            }
    }
}
