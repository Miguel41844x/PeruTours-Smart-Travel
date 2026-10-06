package com.example.perutours.ui.screens.profile

import android.net.Uri
import com.example.perutours.data.model.Preference

data class ProfileUiState(
    val name: String = "",
    val phone: String = "",
    val dni: String = "",
    val city: String = "Lima, Perú",
    val role: String = "cliente",
    val email: String = "",
    val photoUrl: String = "",

    // IDs de preferencias seleccionadas
    val selectedPreferences: Set<String> = emptySet(),

    // Preferencias obtenidas desde Firestore
    val availablePreferences: List<Preference> = emptyList(),

    // Errores de validación
    val nameError: String? = null,
    val phoneError: String? = null,
    val dniError: String? = null,
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