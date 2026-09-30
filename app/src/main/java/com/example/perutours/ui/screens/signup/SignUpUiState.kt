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
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val showVerificationDialog: Boolean = false
)