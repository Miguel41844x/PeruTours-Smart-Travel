package com.example.perutours.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.UserProfile
import com.example.perutours.data.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: ProfileRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    constructor() : this(
        repository = ProfileRepository(),
        auth = FirebaseAuth.getInstance()
    )

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> =
        _uiState.asStateFlow()

    val availablePreferences = listOf(
        "Aventura y Trekking",
        "Historia y Cultura",
        "Naturaleza y Selva",
        "Gastronomía Peruana",
        "Playas y Relax",
        "Turismo Vivencial",
        "Fotografía y Paisajes",
        "Viaje en Familia"
    )

    init {
        loadProfile()
    }

    private fun loadProfile() {

        viewModelScope.launch {

            try {

                val currentUser = auth.currentUser

                val uid = currentUser?.uid ?: ""

                if (uid.isEmpty()) {

                    _uiState.value =
                        _uiState.value.copy(
                            isLoadingInitialData = false,
                            message = "No hay un usuario autenticado."
                        )

                    return@launch
                }

                val profile =
                    repository.getProfile(uid)

                applyProfile(profile)

            } catch (_: Exception) {
                val uid = auth.currentUser?.uid.orEmpty()
                val fallbackProfile = repository.getAuthProfile(uid)

                applyProfile(
                    profile = fallbackProfile,
                    message = "No se pudieron cargar los datos de Firestore. Se muestran los datos de la cuenta."
                )
            }
        }
    }

    private fun applyProfile(
        profile: UserProfile,
        message: String? = null
    ) {
        _uiState.value = _uiState.value.copy(
            name = profile.name,
            email = profile.email,
            phone = profile.phone,
            dni = profile.dni,
            city = profile.city,
            role = profile.role,
            photoUrl = profile.photoUrl,
            selectedPreferences = profile.preferences.toSet(),
            isLoadingInitialData = false,
            message = message
        )
    }

    fun onNameChanged(value: String) {

        _uiState.value =
            _uiState.value.copy(
                name = value,
                nameError = null
            )
    }

    fun onPhoneChanged(value: String) {

        if (
            value.length <= 9 &&
            value.all { it.isDigit() }
        ) {

            _uiState.value =
                _uiState.value.copy(
                    phone = value,
                    phoneError = null
                )
        }
    }

    fun onDniChanged(value: String) {
        _uiState.value =
            _uiState.value.copy(
                dni = value,
                dniError = null
            )
    }

    fun onCityChanged(value: String) {

        _uiState.value =
            _uiState.value.copy(
                city = value,
                cityError = null
            )
    }

    fun togglePreference(preference: String) {

        val current =
            _uiState.value.selectedPreferences

        val updated =
            if (current.contains(preference)) {
                current - preference
            } else {
                current + preference
            }

        _uiState.value =
            _uiState.value.copy(
                selectedPreferences = updated,
                preferencesError = null
            )
    }

    private fun validateFields(): Boolean {
        val state = _uiState.value
        val validation = ProfileValidator.validate(
            name = state.name,
            phone = state.phone,
            dni = state.dni,
            city = state.city,
            selectedPreferences = state.selectedPreferences
        )

        _uiState.value =
            state.copy(
                nameError = validation.nameError,
                phoneError = validation.phoneError,
                dniError = validation.dniError,
                cityError = validation.cityError,
                preferencesError = validation.preferencesError
            )

        return validation.isValid
    }

    fun saveProfile() {

        if (!validateFields()) {

            _uiState.value =
                _uiState.value.copy(
                    message =
                        "Por favor corrige los campos obligatorios marcados en rojo."
                )

            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isSaving = true
                )

            try {

                val currentUser = auth.currentUser

                val uid =
                    currentUser?.uid ?: ""

                if (uid.isEmpty()) {

                    throw Exception(
                        "No hay un usuario autenticado."
                    )
                }

                val state = _uiState.value

                val profile =
                    UserProfile(
                        uid = uid,
                        name = state.name.trim(),
                        email = state.email,
                        phone = state.phone.trim(),
                        dni = state.dni.trim(),
                        city = state.city.trim(),
                        role = state.role,
                        photoUrl = state.photoUrl,
                        preferences =
                            state.selectedPreferences.toList()
                    )

                repository.saveProfile(profile)

                _uiState.value =
                    _uiState.value.copy(
                        isSaving = false,
                        message =
                            "¡Perfil y preferencias guardados con éxito!"
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isSaving = false,
                        message =
                            "Error al guardar en Firestore: ${e.localizedMessage}"
                    )
            }
        }
    }

    fun uploadPhoto(uri: Uri) {

        viewModelScope.launch {

            val currentUser = auth.currentUser

            val uid =
                currentUser?.uid ?: ""

            if (uid.isEmpty()) {

                _uiState.value =
                    _uiState.value.copy(
                        message =
                            "No hay un usuario autenticado."
                    )

                return@launch
            }

            _uiState.value =
                _uiState.value.copy(
                    isUploadingPhoto = true,
                    pendingPhotoUri = uri
                )

            try {

                val remoteUrl =
                    repository.uploadProfilePhoto(
                        uid = uid,
                        uri = uri
                    )

                _uiState.value =
                    _uiState.value.copy(
                        photoUrl = remoteUrl,
                        isUploadingPhoto = false,
                        pendingPhotoUri = null,
                        message =
                            "Foto de perfil guardada en Firebase Storage"
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isUploadingPhoto = false,
                        pendingPhotoUri = null,
                        message =
                            "Error al subir la foto: ${e.localizedMessage}"
                    )
            }
        }
    }

    fun clearMessage() {

        _uiState.value =
            _uiState.value.copy(
                message = null
            )
    }
}
