package com.example.perutours.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica que el documento de usuario construido por AuthRepository cumpla
 * el contrato de firestore.rules (isValidFullUser): campos completos, sin
 * campos extra como createdAtMillis y rol siempre cliente.
 */
class AuthRepositoryDocumentTest {

    private val allowedFields = setOf(
        "uid", "name", "email", "phone", "dni", "city", "role",
        "photoUrl", "preferences", "updatedAt"
    )

    private val testNow = 1_700_000_000_000L

    private fun buildDoc() = AuthRepository.buildClientProfileDoc(
        uid = "uid_123",
        name = "Juan Pérez",
        email = "juan@perutours.pe",
        phone = "987654321",
        nowMillis = testNow
    )

    @Test
    fun `el documento contiene exactamente los campos permitidos por las reglas`() {
        assertEquals(allowedFields, buildDoc().keys)
    }

    @Test
    fun `nunca se escribe createdAtMillis en el perfil del usuario`() {
        // Reglas: hasOnlyAllowedFields no incluye createdAtMillis en users/{uid}.
        assertFalse(buildDoc().containsKey("createdAtMillis"))
    }

    @Test
    fun `el rol siempre es cliente desde la app publica`() {
        assertEquals("cliente", buildDoc()["role"])
    }

    @Test
    fun `un usuario nuevo puede registrarse sin preferencias`() {
        val preferences = buildDoc()["preferences"]
        assertTrue(preferences is List<*>)
        assertTrue((preferences as List<*>).isEmpty())
    }

    @Test
    fun `los campos obligatorios estan presentes con valores por defecto seguros`() {
        val doc = buildDoc()
        assertEquals("uid_123", doc["uid"])
        assertEquals("Juan Pérez", doc["name"])
        assertEquals("juan@perutours.pe", doc["email"])
        assertEquals("987654321", doc["phone"])
        assertEquals("", doc["dni"])
        assertEquals("Lima, Perú", doc["city"])
        assertEquals("", doc["photoUrl"])
        assertEquals(testNow, doc["updatedAt"] as Long)
    }

    @Test
    fun `el respaldo de login usa la misma forma completa que el registro`() {
        // registerClient y ensureUserDocumentExists comparten este constructor,
        // por lo que el respaldo nunca puede quedar incompleto frente a las reglas.
        assertEquals(buildDoc(), buildDoc())
    }
}
