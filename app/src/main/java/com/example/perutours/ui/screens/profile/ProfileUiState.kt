package com.example.perutours.ui.screens.profile

import android.net.Uri
import com.example.perutours.data.model.Preference

sealed interface PhotoUploadState {
    object Idle : PhotoUploadState
    object Uploading : PhotoUploadState
    data class Success(val remoteUrl: String) : PhotoUploadState
    data class Error(val message: String) : PhotoUploadState
}

data class ProfileUiState(
    val name: String = "",
    val phone: String = "",
    val dni: String = "",
    val city: String = "Lima, Perú",
    val role: String = "cliente",
    val email: String = "",

    // Separación de foto local, estado de subida y URL remota
    val localPhotoUri: Uri? = null,
    val photoUrl: String = "",
    val photoUploadState: PhotoUploadState = PhotoUploadState.Idle,

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

    // Mensaje para Snackbar
    val message: String? = null
) {
    val isUploadingPhoto: Boolean
        get() = photoUploadState is PhotoUploadState.Uploading

    @Deprecated("Usar localPhotoUri", ReplaceWith("localPhotoUri"))
    val pendingPhotoUri: Uri?
        get() = localPhotoUri
}
