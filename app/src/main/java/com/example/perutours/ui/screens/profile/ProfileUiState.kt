package com.example.perutours.ui.screens.profile

import android.net.Uri

data class ProfileUiState(
    val name: String = "",
    val phone: String = "",
    val dni: String = "",
    val city: String = "Lima, Perú",
    val role: String = "cliente",
    val email: String = "",
    val photoUrl: String = "",

    val selectedPreferences: Set<String> = emptySet(),

    // Errores de validación
    val nameError: String? = null,
    val phoneError: String? = null,
    val cityError: String? = null,
    val preferencesError: String? = null,

    // Estados de carga
    val isLoadingInitialData: Boolean = true,
    val isSaving: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val pendingPhotoUri: Uri? = null,

    // Mensaje para Snackbar
    val message: String? = null
)
