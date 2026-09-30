package com.example.perutours.ui.screens.signup

data class SignUpUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val selectedRoleId: String = "cliente",

    // Errores específicos por campo validados con SignUpValidator
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,

    // Error global de backend / Firebase
    val generalError: String? = null,

    // Estados de carga y flujo
    val isLoading: Boolean = false,
    val showVerificationDialog: Boolean = false
)