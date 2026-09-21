package com.perutours.smarttravel.presentation.auth

import com.perutours.smarttravel.domain.model.AuthUser

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Authenticated(val user: AuthUser) : AuthUiState
    data class Error(val message: String) : AuthUiState
    data object Unauthenticated : AuthUiState
    data class EmailNotVerified(val user: AuthUser) : AuthUiState
}

sealed interface RegisterUiState {
    data object Idle : RegisterUiState
    data object Loading : RegisterUiState
    data class Success(val message: String) : RegisterUiState
    data class Error(val message: String) : RegisterUiState
}
