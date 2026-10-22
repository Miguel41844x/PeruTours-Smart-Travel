package com.example.perutours.ui.screens.admin.provider

import com.example.perutours.data.model.TourProvider

/**
 * Validación del formulario de proveedores del catálogo.
 * Se separa del ViewModel para poder probarla con JUnit.
 */
object ProviderFormValidator {

    fun validateName(name: String): String? = when {
        name.trim().length < 2 -> "El nombre debe tener al menos 2 caracteres."
        name.trim().length > 120 -> "El nombre supera los 120 caracteres."
        else -> null
    }

    fun validateCity(city: String): String? = when {
        city.trim().length < 2 -> "Indica la ciudad o región del proveedor."
        city.trim().length > 100 -> "La ciudad supera los 100 caracteres."
        else -> null
    }

    fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        val pattern = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")
        return when {
            trimmed.isEmpty() -> "Indica un correo de contacto."
            !pattern.matches(trimmed) -> "El correo no tiene un formato válido."
            else -> null
        }
    }

    /** Teléfono fijo o celular peruano; admite espacios y el prefijo +51. */
    fun validatePhone(phone: String): String? {
        val digits = phone.filter { it.isDigit() }
        val valid = phone.isBlank() || digits.length in 7..12
        return if (valid) null else "El teléfono debe tener entre 7 y 12 dígitos."
    }

    fun parseServices(raw: String): List<String> = raw
        .split(',', '·', '/', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .take(10)

    fun buildProvider(
        existing: TourProvider?,
        name: String,
        contactName: String,
        phone: String,
        email: String,
        city: String,
        servicesText: String,
        active: Boolean,
        nowMillis: Long
    ): TourProvider {
        val base = existing ?: TourProvider()
        return base.copy(
            name = name.trim(),
            contactName = contactName.trim(),
            phone = phone.trim(),
            email = email.trim(),
            city = city.trim(),
            serviceNames = parseServices(servicesText),
            active = active,
            createdAtMillis = base.createdAtMillis.takeIf { it > 0L } ?: nowMillis,
            updatedAtMillis = nowMillis
        )
    }
}
