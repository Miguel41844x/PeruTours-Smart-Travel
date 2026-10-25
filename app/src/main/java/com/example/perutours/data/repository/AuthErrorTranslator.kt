package com.example.perutours.data.repository

import com.google.firebase.auth.FirebaseAuthException
import java.util.Locale

/**
 * Funciones del proceso de autenticación que muestran un mensaje al usuario.
 * Cada una tiene sus propios mensajes porque un mismo error de Firebase
 * significa cosas distintas según lo que esté haciendo la persona.
 */
enum class AuthOperation {
    LOGIN,
    REGISTRO,
    RESTABLECER_CONTRASENA
}

/**
 * Traduce los errores de Firebase Authentication al español.
 *
 * Firebase devuelve mensajes genéricos y en inglés
 * ("The supplied auth credential is incorrect, malformed or has expired."),
 * por eso se interpreta primero el código de error (`FirebaseAuthException.errorCode`)
 * y, si no existe, el texto del mensaje como respaldo. En el peor de los casos
 * se devuelve un mensaje genérico por operación: nunca se le muestra al usuario
 * texto original del autenticador.
 */
object AuthErrorTranslator {

    private const val MAX_CAUSE_DEPTH = 5

    private const val FORMATO_CORREO = "El formato del correo electrónico no es válido."

    /** Códigos que significan "no coinciden las credenciales". */
    private val CREDENTIAL_CODES = setOf(
        "INVALID_CREDENTIAL",
        "WRONG_PASSWORD",
        "INVALID_PASSWORD"
    )

    /**
     * Punto de entrada: traduce la excepción original conservando su causa.
     */
    fun translate(error: Throwable?, operation: AuthOperation): String =
        translate(resolveCode(error), error?.message, operation)

    /**
     * Versión comprobable sin excepciones: recibe el código de Firebase
     * (por ejemplo "ERROR_EMAIL_ALREADY_IN_USE") y el mensaje original.
     */
    fun translate(code: String?, message: String?, operation: AuthOperation): String =
        code?.let { fromCode(normalize(it), operation) }
            ?: fromMessage(message.orEmpty(), operation)
            ?: genericMessage(operation)

    // ---------------------------------------------------------------------
    // 1. Código de error de Firebase (fuente confiable)
    // ---------------------------------------------------------------------

    private fun fromCode(code: String, operation: AuthOperation): String? = when (code) {
        in CREDENTIAL_CODES -> credentialsMessage(operation)
        "MISSING_PASSWORD" -> "La contraseña es obligatoria."
        "MISSING_EMAIL" -> "El correo electrónico es obligatorio."
        "INVALID_EMAIL", "INVALID_RECIPIENT_EMAIL" -> FORMATO_CORREO
        "USER_NOT_FOUND" -> userNotFoundMessage(operation)
        "EMAIL_ALREADY_IN_USE",
        "ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL",
        "CREDENTIAL_ALREADY_IN_USE" -> emailInUseMessage(operation)
        "WEAK_PASSWORD" -> weakPasswordMessage(operation)
        "USER_DISABLED" -> userDisabledMessage(operation)
        "TOO_MANY_REQUESTS" -> tooManyRequestsMessage(operation)
        "NETWORK_REQUEST_FAILED" -> networkMessage(operation)
        "OPERATION_NOT_ALLOWED" -> operationNotAllowedMessage(operation)
        "INVALID_USER_TOKEN",
        "USER_TOKEN_EXPIRED",
        "USER_MISMATCH" -> expiredSessionMessage(operation)
        "REQUIRES_RECENT_LOGIN" -> recentLoginMessage(operation)
        else -> null
    }

    /** "ERROR_INVALID_EMAIL" -> "INVALID_EMAIL", "auth/invalid-email" -> "INVALID_EMAIL". */
    private fun normalize(code: String): String =
        code.trim()
            .uppercase(Locale.ROOT)
            .removePrefix("ERROR_")
            .removePrefix("FIREBASE_")
            .removePrefix("AUTH/")
            .replace('-', '_')
            .replace(' ', '_')

    private fun resolveCode(error: Throwable?): String? {
        var current: Throwable? = error
        var depth = 0
        while (current != null && depth <= MAX_CAUSE_DEPTH) {
            if (current is FirebaseAuthException) return current.errorCode
            current = current.cause
            depth++
        }
        return null
    }

    // ---------------------------------------------------------------------
    // 2. Respaldo: traducción a partir del texto original en inglés
    // ---------------------------------------------------------------------

    private fun fromMessage(message: String, operation: AuthOperation): String? {
        val text = message.lowercase(Locale.ROOT)
        if (text.isBlank()) return null

        return when {
            text.contains("password is invalid or the user") -> credentialsMessage(operation)
            text.contains("credential is incorrect") ||
                text.contains("credential is malformed") ||
                text.contains("credential is expired") -> credentialsMessage(operation)

            text.contains("no user record") ||
                text.contains("there is no user") -> userNotFoundMessage(operation)

            text.contains("already in use") ||
                text.contains("account already exists") ||
                text.contains("credential is already associated") -> emailInUseMessage(operation)

            text.contains("badly formatted") -> FORMATO_CORREO

            text.contains("password should be at least") ||
                text.contains("given password is invalid") -> weakPasswordMessage(operation)

            text.contains("disabled by an administrator") ||
                text.contains("account has been disabled") -> userDisabledMessage(operation)

            text.contains("blocked all requests") ||
                text.contains("too many requests") -> tooManyRequestsMessage(operation)

            text.contains("network error") ||
                text.contains("unreachable host") ||
                text.contains("timeout") ||
                text.contains("deadline exceeded") -> networkMessage(operation)

            text.contains("operation is not allowed") ||
                text.contains("sign-in provider is disabled") -> operationNotAllowedMessage(operation)

            text.contains("requires recent authentication") -> recentLoginMessage(operation)

            text.contains("permission denied") ||
                text.contains("insufficient permissions") -> permissionMessage(operation)

            text.contains("session") &&
                (text.contains("expired") || text.contains("no longer valid")) -> expiredSessionMessage(operation)

            else -> null
        }
    }

    // ---------------------------------------------------------------------
    // 3. Mensajes por operación
    // ---------------------------------------------------------------------

    private fun credentialsMessage(operation: AuthOperation): String = when (operation) {
        AuthOperation.LOGIN ->
            "El correo o la contraseña son incorrectos. Verifica tus datos e inténtalo de nuevo."
        AuthOperation.REGISTRO ->
            "No se pudo validar la cuenta. Revisa el correo y la contraseña e inténtalo de nuevo."
        AuthOperation.RESTABLECER_CONTRASENA ->
            "No se pudo validar el correo electrónico. Verifícalo e inténtalo de nuevo."
    }

    private fun userNotFoundMessage(operation: AuthOperation): String = when (operation) {
        AuthOperation.LOGIN ->
            "No existe ninguna cuenta con este correo electrónico. Crea una cuenta o revisa que esté bien escrito."
        AuthOperation.REGISTRO ->
            "No se encontró la cuenta asociada a este correo. Intenta crear la cuenta de nuevo."
        AuthOperation.RESTABLECER_CONTRASENA ->
            "No encontramos ninguna cuenta asociada a este correo electrónico. Crea una cuenta si aún no tienes una."
    }

    private fun emailInUseMessage(operation: AuthOperation): String = when (operation) {
        AuthOperation.LOGIN ->
            "Este correo electrónico ya está registrado. ¿Olvidaste tu contraseña? Restablécela desde aquí."
        AuthOperation.REGISTRO ->
            "Este correo electrónico ya está registrado. Si olvidaste tu contraseña, restablécela desde Iniciar sesión."
        AuthOperation.RESTABLECER_CONTRASENA ->
            "Ya existe una cuenta con este correo electrónico. Vuelve a Iniciar sesión para restablecer tu contraseña."
    }

    private fun weakPasswordMessage(operation: AuthOperation): String = when (operation) {
        AuthOperation.REGISTRO ->
            "La contraseña es muy débil: debe tener al menos 6 caracteres e incluir una mayúscula, un número y un símbolo."
        AuthOperation.LOGIN ->
            "La contraseña no cumple los requisitos mínimos: al menos 6 caracteres, una mayúscula, un número y un símbolo."
        AuthOperation.RESTABLECER_CONTRASENA ->
            "La contraseña no cumple los requisitos mínimos: al menos 6 caracteres, una mayúscula, un número y un símbolo."
    }

    private fun userDisabledMessage(operation: AuthOperation): String = when (operation) {
        AuthOperation.LOGIN ->
            "Esta cuenta está deshabilitada. Contacta con soporte para reactivarla."
        AuthOperation.REGISTRO ->
            "Esta cuenta fue deshabilitada por un administrador. Contacta con soporte."
        AuthOperation.RESTABLECER_CONTRASENA ->
            "Esta cuenta está deshabilitada, por eso no podemos enviar el correo de restablecimiento."
    }

    private fun tooManyRequestsMessage(operation: AuthOperation): String = when (operation) {
        AuthOperation.LOGIN ->
            "Demasiados intentos fallidos. Espera unos minutos y vuelve a intentarlo."
        AuthOperation.REGISTRO ->
            "Demasiados intentos de registro. Espera unos minutos y vuelve a intentarlo."
        AuthOperation.RESTABLECER_CONTRASENA ->
            "Solicitaste varios enlaces seguidos. Espera unos minutos para recibir el siguiente correo."
    }

    private fun networkMessage(operation: AuthOperation): String = when (operation) {
        AuthOperation.LOGIN ->
            "No se pudo conectar con el servidor. Revisa tu conexión a internet e inténtalo de nuevo."
        AuthOperation.REGISTRO ->
            "No se pudo conectar con el servidor. Revisa tu conexión a internet e inténtalo de nuevo."
        AuthOperation.RESTABLECER_CONTRASENA ->
            "No se pudo enviar el correo. Revisa tu conexión a internet e inténtalo de nuevo."
    }

    private fun operationNotAllowedMessage(operation: AuthOperation): String = when (operation) {
        AuthOperation.LOGIN ->
            "El inicio de sesión con correo y contraseña no está habilitado para esta aplicación."
        AuthOperation.REGISTRO ->
            "El registro con correo y contraseña no está habilitado para esta aplicación."
        AuthOperation.RESTABLECER_CONTRASENA ->
            "El restablecimiento de contraseña no está habilitado para esta aplicación."
    }

    private fun expiredSessionMessage(operation: AuthOperation): String = when (operation) {
        AuthOperation.LOGIN ->
            "Tu sesión ya no es válida. Inicia sesión nuevamente para continuar."
        AuthOperation.REGISTRO ->
            "La sesión anterior ya no es válida. Intenta crear la cuenta de nuevo."
        AuthOperation.RESTABLECER_CONTRASENA ->
            "Tu sesión ya no es válida. Vuelve a intentarlo desde Iniciar sesión."
    }

    private fun recentLoginMessage(operation: AuthOperation): String = when (operation) {
        AuthOperation.LOGIN -> "Por seguridad, vuelve a iniciar sesión para continuar."
        AuthOperation.REGISTRO -> "Por seguridad, vuelve a iniciar sesión para continuar."
        AuthOperation.RESTABLECER_CONTRASENA ->
            "Por seguridad, vuelve a iniciar sesión antes de solicitar el enlace."
    }

    private fun permissionMessage(operation: AuthOperation): String = when (operation) {
        AuthOperation.LOGIN ->
            "No tienes permisos para iniciar sesión con esta cuenta. Contacta con soporte."
        AuthOperation.REGISTRO ->
            "No tienes permisos para crear la cuenta. Contacta con soporte."
        AuthOperation.RESTABLECER_CONTRASENA ->
            "No tienes permisos para restablecer la contraseña. Contacta con soporte."
    }

    private fun genericMessage(operation: AuthOperation): String = when (operation) {
        AuthOperation.LOGIN ->
            "No pudimos iniciar sesión. Revisa tu correo y contraseña e inténtalo de nuevo en unos minutos."
        AuthOperation.REGISTRO ->
            "No pudimos crear tu cuenta. Revisa tus datos e inténtalo de nuevo en unos minutos."
        AuthOperation.RESTABLECER_CONTRASENA ->
            "No pudimos enviar el correo de restablecimiento. Intenta de nuevo en unos minutos."
    }
}
