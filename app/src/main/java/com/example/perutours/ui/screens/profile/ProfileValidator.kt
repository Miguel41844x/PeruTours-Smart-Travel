package com.example.perutours.ui.screens.profile

import com.example.perutours.data.model.TravelProfileOptions

data class ProfileValidationResult(
    val nameError: String? = null,
    val phoneError: String? = null,
    val dniError: String? = null,
    val cityError: String? = null,
    val preferencesError: String? = null,
    val tripTypeError: String? = null,
    val budgetError: String? = null,
    val durationError: String? = null
) {
    val isValid: Boolean
        get() = nameError == null &&
                phoneError == null &&
                dniError == null &&
                cityError == null &&
                preferencesError == null &&
                tripTypeError == null &&
                budgetError == null &&
                durationError == null
}

object ProfileValidator {

    fun validate(
        name: String,
        phone: String,
        dni: String,
        city: String,
        selectedPreferences: Set<String>,
        tripType: String,
        budget: String,
        duration: String
    ): ProfileValidationResult {

        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        val cleanDni = dni.trim()
        val cleanCity = city.trim()
        val cleanTripType = tripType.trim()
        val cleanBudget = budget.trim()
        val cleanDuration = duration.trim()

        // Nombre: letras, espacios y caracteres propios del español
        val nameRegex =
            Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]+$")

        // Ciudad / país: letras, espacios, tildes y separadores comunes
        val cityRegex =
            Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ,.-]+$")

        // Pasaporte: letras y números
        val passportRegex =
            Regex("^[a-zA-Z0-9]+$")

        val nameError = when {
            cleanName.isBlank() ->
                "El nombre completo es obligatorio."

            cleanName.length < 3 ->
                "El nombre debe tener al menos 3 caracteres."

            !nameRegex.matches(cleanName) ->
                "El nombre solo puede contener letras y espacios."

            else -> null
        }

        val phoneError = when {
            cleanPhone.isBlank() ->
                "El número de celular es obligatorio."

            cleanPhone.length != 9 ->
                "El celular debe tener exactamente 9 dígitos."

            !cleanPhone.all { it.isDigit() } ->
                "El celular solo puede contener números."

            !cleanPhone.startsWith("9") ->
                "El celular debe comenzar con 9."

            else -> null
        }

        /*
         * DNI / Pasaporte es OPCIONAL.
         *
         * Si está vacío → válido.
         * Si tiene 8 dígitos → se considera DNI válido.
         * Si tiene formato alfanumérico → se considera pasaporte válido.
         */
        val dniError = when {
            cleanDni.isBlank() -> {
                null
            }

            cleanDni.length == 8 &&
                    cleanDni.all { it.isDigit() } -> {
                null
            }

            cleanDni.length in 6..12 &&
                    passportRegex.matches(cleanDni) &&
                    cleanDni.any { it.isLetter() } &&
                    cleanDni.any { it.isDigit() } -> {
                null
            }

            else -> {
                "Ingresa un DNI válido de 8 dígitos o un pasaporte válido."
            }
        }

        val cityError = when {
            cleanCity.isBlank() ->
                "La ciudad o país de origen es obligatorio."

            cleanCity.length < 2 ->
                "Ingresa una ciudad o país válido."

            !cityRegex.matches(cleanCity) ->
                "La ciudad o país solo puede contener letras y caracteres válidos."

            else -> null
        }

        val preferencesError =
            if (selectedPreferences.isEmpty()) {
                "Selecciona al menos una preferencia de viaje."
            } else {
                null
            }

        // HU10: tipo de viaje, presupuesto y duración son obligatorios
        // y solo aceptan los valores permitidos (TravelProfileOptions).
        val tripTypeError =
            if (cleanTripType in TravelProfileOptions.tripTypes) {
                null
            } else {
                "Selecciona tu tipo de viaje."
            }

        val budgetError =
            if (cleanBudget in TravelProfileOptions.budgets) {
                null
            } else {
                "Selecciona tu presupuesto de viaje."
            }

        val durationError =
            if (cleanDuration in TravelProfileOptions.durations) {
                null
            } else {
                "Selecciona la duración de tu viaje."
            }

        return ProfileValidationResult(
            nameError = nameError,
            phoneError = phoneError,
            dniError = dniError,
            cityError = cityError,
            preferencesError = preferencesError,
            tripTypeError = tripTypeError,
            budgetError = budgetError,
            durationError = durationError
        )
    }
}
