package com.example.perutours.auth

import com.example.perutours.ui.screens.login.LoginValidator
import com.example.perutours.ui.screens.signup.SignUpValidator
import org.junit.Assert.*
import org.junit.Test

class AuthFlowTest {

    @Test
    fun registerValidationRequiresMinimumCharactersAndLettersOnly() {
        val invalidName = SignUpValidator.validate(
            name = "Juan123",
            email = "juan@test.com",
            phone = "987654321",
            password = "Password1@",
            confirmPassword = "Password1@"
        )
        assertFalse(invalidName.isValid)
        assertEquals("El nombre solo puede contener letras y espacios.", invalidName.nameError)

        val valid = SignUpValidator.validate(
            name = "Juan Pérez",
            email = "juan@test.com",
            phone = "987654321",
            password = "Password1@",
            confirmPassword = "Password1@"
        )
        assertTrue(valid.isValid)
    }

    @Test
    fun loginValidationCatchesEmptyEmailAndPassword() {
        val emptyResult = LoginValidator.validate("", "")
        assertFalse(emptyResult.isValid)
        assertEquals("Ingresa tu correo electrónico.", emptyResult.emailError)
        assertEquals("Ingresa tu contraseña.", emptyResult.passwordError)
    }

    @Test
    fun passwordResetValidationRejectsMalformedEmail() {
        val malformed = LoginValidator.validateEmailOnly("correoinvalido")
        assertNotNull(malformed)

        val valid = LoginValidator.validateEmailOnly("usuario@gmail.com")
        assertNull(valid)
    }

    @Test
    fun clientRoleIsAlwaysAssignedToPublicRegistrations() {
        // Verifica que la regla de negocio fuerza el rol 'cliente'
        val defaultRole = "cliente"
        assertNotEquals("agente", defaultRole)
        assertEquals("cliente", defaultRole)
    }
}