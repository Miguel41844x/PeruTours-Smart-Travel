package com.example.perutours.ui.screens.login

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,

    // Errores específicos por campo validados con LoginValidator
    val emailError: String? = null,
    val passwordError: String? = null,

    // Mensajes generales (éxito en verde o error en rojo)
    val infoMessage: String? = null,
    val isSuccessMessage: Boolean = false,

    // Estado de carga para el botón
    val isLoading: Boolean = false
)
