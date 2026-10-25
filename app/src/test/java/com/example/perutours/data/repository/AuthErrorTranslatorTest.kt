package com.example.perutours.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprueba que cualquier error del autenticador se muestre en español y,
 * sobre todo, que el mensaje explique lo que falló en la función que la
 * persona estaba usando: iniciar sesión, registrarse o restablecer la contraseña.
 */
class AuthErrorTranslatorTest {

    private val operaciones = AuthOperation.values()

    private val codigosReales = listOf(
        "ERROR_INVALID_CREDENTIAL",
        "ERROR_WRONG_PASSWORD",
        "ERROR_USER_NOT_FOUND",
        "ERROR_EMAIL_ALREADY_IN_USE",
        "ERROR_WEAK_PASSWORD",
        "ERROR_INVALID_EMAIL",
        "ERROR_USER_DISABLED",
        "ERROR_TOO_MANY_REQUESTS",
        "ERROR_NETWORK_REQUEST_FAILED",
        "ERROR_OPERATION_NOT_ALLOWED",
        "ERROR_INVALID_USER_TOKEN",
        "ERROR_USER_TOKEN_EXPIRED",
        "ERROR_REQUIRES_RECENT_LOGIN",
        "ERROR_INTERNAL_ERROR",
        "ERROR_DE_UNA_FUNCION_NUEVA_QUE_TODAVIA_NO_CONOCEMOS"
    )

    @Test
    fun `una contrasena incorrecta se explica en español al iniciar sesión`() {
        val mensaje = AuthErrorTranslator.translate(
            "ERROR_WRONG_PASSWORD", null, AuthOperation.LOGIN
        )

        assertEquals(
            "El correo o la contraseña son incorrectos. Verifica tus datos e inténtalo de nuevo.",
            mensaje
        )
    }

    @Test
    fun `la credencial inválida se traduce por código y también por su texto original`() {
        val esperado =
            "El correo o la contraseña son incorrectos. Verifica tus datos e inténtalo de nuevo."

        val porCodigo = AuthErrorTranslator.translate(
            "ERROR_INVALID_CREDENTIAL", null, AuthOperation.LOGIN
        )
        val porTexto = AuthErrorTranslator.translate(
            null,
            "The supplied auth credential is incorrect, malformed or has expired.",
            AuthOperation.LOGIN
        )

        assertEquals(esperado, porCodigo)
        assertEquals(esperado, porTexto)
    }

    @Test
    fun `el correo ya registrado se explica de forma distinta al iniciar sesión y al registrarse`() {
        val alRegistrarse = AuthErrorTranslator.translate(
            "ERROR_EMAIL_ALREADY_IN_USE", null, AuthOperation.REGISTRO
        )
        val alIniciarSesion = AuthErrorTranslator.translate(
            "ERROR_EMAIL_ALREADY_IN_USE", null, AuthOperation.LOGIN
        )

        assertTrue(alRegistrarse.startsWith("Este correo electrónico ya está registrado."))
        assertTrue(alRegistrarse.contains("restablécela desde Iniciar sesión"))
        assertTrue(alIniciarSesion.contains("¿Olvidaste tu contraseña?"))
        assertFalse(alIniciarSesion.contains("Error"))
    }

    @Test
    fun `una cuenta inexistente avisa distinto al login que al restablecer la contraseña`() {
        val alIniciarSesion = AuthErrorTranslator.translate(
            "ERROR_USER_NOT_FOUND", null, AuthOperation.LOGIN
        )
        val alRestablecer = AuthErrorTranslator.translate(
            "ERROR_USER_NOT_FOUND", null, AuthOperation.RESTABLECER_CONTRASENA
        )

        assertTrue(alIniciarSesion.startsWith("No existe ninguna cuenta con este correo"))
        assertTrue(alRestablecer.startsWith("No encontramos ninguna cuenta asociada"))
        assertTrue(alIniciarSesion.contains("Crea una cuenta"))
        assertTrue(alRestablecer.contains("Crea una cuenta si aún no tienes una"))
    }

    @Test
    fun `una contraseña débil recuerda las reglas que exige el registro`() {
        val mensaje = AuthErrorTranslator.translate(
            "ERROR_WEAK_PASSWORD", null, AuthOperation.REGISTRO
        )

        assertTrue(mensaje.contains("al menos 6 caracteres"))
        assertTrue(mensaje.contains("mayúscula"))
        assertTrue(mensaje.contains("número"))
        assertTrue(mensaje.contains("símbolo"))
    }

    @Test
    fun `el límite de intentos se explica según la función en curso`() {
        val alIniciarSesion = AuthErrorTranslator.translate(
            "ERROR_TOO_MANY_REQUESTS", null, AuthOperation.LOGIN
        )
        val alRegistrarse = AuthErrorTranslator.translate(
            "ERROR_TOO_MANY_REQUESTS", null, AuthOperation.REGISTRO
        )
        val alRestablecer = AuthErrorTranslator.translate(
            "ERROR_TOO_MANY_REQUESTS", null, AuthOperation.RESTABLECER_CONTRASENA
        )

        assertEquals("Demasiados intentos fallidos. Espera unos minutos y vuelve a intentarlo.", alIniciarSesion)
        assertEquals("Demasiados intentos de registro. Espera unos minutos y vuelve a intentarlo.", alRegistrarse)
        assertEquals(
            "Solicitaste varios enlaces seguidos. Espera unos minutos para recibir el siguiente correo.",
            alRestablecer
        )
    }

    @Test
    fun `un error de conexión siempre avisa que revise internet`() {
        for (operacion in operaciones) {
            val mensaje = AuthErrorTranslator.translate(
                "ERROR_NETWORK_REQUEST_FAILED", null, operacion
            )
            assertTrue("Falta aviso de conexión en $operacion", mensaje.contains("internet"))
        }
    }

    @Test
    fun `el formato de correo inválido se dice igual en todas las funciones`() {
        val formasDeEscribirElCodigo = listOf(
            "ERROR_INVALID_EMAIL",
            "INVALID_EMAIL",
            "auth/invalid-email",
            "invalid-email",
            " error_invalid_email "
        )

        for (codigo in formasDeEscribirElCodigo) {
            for (operacion in operaciones) {
                assertEquals(
                    "Formato mal traducido para '$codigo' en $operacion",
                    "El formato del correo electrónico no es válido.",
                    AuthErrorTranslator.translate(codigo, null, operacion)
                )
            }
        }
    }

    @Test
    fun `sin código de Firebase se recurre al texto original en inglés`() {
        val mensaje = AuthErrorTranslator.translate(
            null,
            "The email address is badly formatted.",
            AuthOperation.LOGIN
        )

        assertEquals("El formato del correo electrónico no es válido.", mensaje)
    }

    @Test
    fun `sin código y sin mensaje se usa el texto genérico de cada operación`() {
        assertEquals(
            "No pudimos iniciar sesión. Revisa tu correo y contraseña e inténtalo de nuevo en unos minutos.",
            AuthErrorTranslator.translate(null, null, AuthOperation.LOGIN)
        )
        assertEquals(
            "No pudimos crear tu cuenta. Revisa tus datos e inténtalo de nuevo en unos minutos.",
            AuthErrorTranslator.translate(null, "", AuthOperation.REGISTRO)
        )
        assertEquals(
            "No pudimos enviar el correo de restablecimiento. Intenta de nuevo en unos minutos.",
            AuthErrorTranslator.translate(null, "   ", AuthOperation.RESTABLECER_CONTRASENA)
        )
    }

    @Test
    fun `ningún mensaje devuelve el texto original del autenticador en inglés`() {
        val palabrasEnIngles = listOf(
            "the ", "please", "credential", "malformed", "supplied", "expired",
            "there is no", "already in use by", "network error", "too many requests",
            "password is", "occurred", "error:"
        )

        for (codigo in codigosReales) {
            for (operacion in operaciones) {
                val mensaje = AuthErrorTranslator.translate(codigo, null, operacion)

                assertTrue("Mensaje vacío para $codigo en $operacion", mensaje.isNotBlank())
                assertFalse(
                    "Texto en inglés para $codigo en $operacion: $mensaje",
                    palabrasEnIngles.any { mensaje.lowercase().contains(it) }
                )
            }
        }
    }

    @Test
    fun `un código futuro que aún no conocemos cae en un mensaje genérico en español`() {
        val mensaje = AuthErrorTranslator.translate(
            "ERROR_DE_UNA_FUNCION_NUEVA_QUE_TODAVIA_NO_CONOCEMOS", null, AuthOperation.REGISTRO
        )

        assertTrue(mensaje.startsWith("No pudimos crear tu cuenta."))
    }
}
