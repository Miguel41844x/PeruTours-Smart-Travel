package com.example.perutours.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.UserProfile
import com.example.perutours.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val repository = ProfileRepository()

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

                val currentUser =
                    com.google.firebase.auth.FirebaseAuth
                        .getInstance()
                        .currentUser

                val uid = currentUser?.uid ?: ""

                if (uid.isEmpty()) {

                    _uiState.value =
                        _uiState.value.copy(
                            isLoadingInitialData = false
                        )

                    return@launch
                }

                val profile =
                    repository.getProfile(uid)

                _uiState.value =
                    _uiState.value.copy(
                        name = profile.name,
                        email = profile.email,
                        phone = profile.phone,
                        dni = profile.dni,
                        city = profile.city,
                        role = profile.role,
                        photoUrl = profile.photoUrl,
                        selectedPreferences =
                            profile.preferences.toSet(),
                        isLoadingInitialData = false
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoadingInitialData = false
                    )
            }
        }
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
                dni = value
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

        val cleanName = state.name.trim()
        val cleanPhone = state.phone.trim()
        val cleanCity = state.city.trim()

        var hasError = false

        var nameError: String? = null
        var phoneError: String? = null
        var cityError: String? = null
        var preferencesError: String? = null

        // Nombre
        if (cleanName.length < 3) {

            nameError =
                "Ingresa tu nombre completo (mínimo 3 caracteres)."

            hasError = true
        }

        // Teléfono
        if (
            cleanPhone.length != 9 ||
            !cleanPhone.startsWith("9")
        ) {

            phoneError =
                "Ingresa un celular válido de 9 dígitos que empiece con 9."

            hasError = true
        }

        // Ciudad
        if (cleanCity.isBlank()) {

            cityError =
                "La ciudad o país de origen es obligatorio."

            hasError = true
        }

        // Preferencias
        // Mantenemos el comportamiento ORIGINAL:
        // mínimo 1 preferencia.
        if (state.selectedPreferences.isEmpty()) {

            preferencesError =
                "Selecciona al menos una preferencia de viaje."

            hasError = true
        }

        _uiState.value =
            state.copy(
                nameError = nameError,
                phoneError = phoneError,
                cityError = cityError,
                preferencesError = preferencesError
            )

        return !hasError
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

                val currentUser =
                    com.google.firebase.auth.FirebaseAuth
                        .getInstance()
                        .currentUser

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

            val currentUser =
                com.google.firebase.auth.FirebaseAuth
                    .getInstance()
                    .currentUser

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
                    isUploadingPhoto = true
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
                        message =
                            "Foto de perfil guardada en Firebase Storage"
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isUploadingPhoto = false,
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