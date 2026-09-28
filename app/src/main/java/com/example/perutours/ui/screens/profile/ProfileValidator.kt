package com.example.perutours.ui.screens.profile

data class ProfileValidationResult(
    val nameError: String? = null,
    val phoneError: String? = null,
    val cityError: String? = null,
    val preferencesError: String? = null
) {
    val isValid: Boolean
        get() = nameError == null &&
            phoneError == null &&
            cityError == null &&
            preferencesError == null
}

object ProfileValidator {
    fun validate(
        name: String,
        phone: String,
        city: String,
        selectedPreferences: Set<String>
    ): ProfileValidationResult {
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        val cleanCity = city.trim()

        return ProfileValidationResult(
            nameError = if (cleanName.length < 3) {
                "Ingresa tu nombre completo (mínimo 3 caracteres)."
            } else {
                null
            },
            phoneError = if (cleanPhone.length != 9 || !cleanPhone.startsWith("9")) {
                "Ingresa un celular válido de 9 dígitos que empiece con 9."
            } else {
                null
            },
            cityError = if (cleanCity.isBlank()) {
                "La ciudad o país de origen es obligatorio."
            } else {
                null
            },
            preferencesError = if (selectedPreferences.isEmpty()) {
                "Selecciona al menos una preferencia de viaje."
            } else {
                null
            }
        )
    }
}
