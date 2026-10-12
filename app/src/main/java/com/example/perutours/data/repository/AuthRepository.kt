package com.example.perutours.data.repository

import com.example.perutours.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isUserLoggedInAndVerified: Boolean
        get() = auth.currentUser != null && auth.currentUser!!.isEmailVerified

    /**
     * Registra al usuario en Firebase Auth y crea automáticamente el documento
     * completo en users/{uid} con role = "cliente" para evitar usuarios bootstrap huérfanos.
     */
    suspend fun registerClient(
        name: String,
        email: String,
        password: String,
        phone: String
    ): Result<Unit> = runCatching {
        val cleanEmail = email.trim()
        val cleanPass = password.trim()
        val cleanName = name.trim()
        val cleanPhone = phone.trim()

        val authResult = auth.createUserWithEmailAndPassword(cleanEmail, cleanPass).await()
        val user = authResult.user ?: throw IllegalStateException("No se pudo obtener el usuario creado.")

        // 1. Actualizar displayName en Firebase Auth
        val profileUpdates = userProfileChangeRequest {
            displayName = "$cleanName | cliente | $cleanPhone"
        }
        user.updateProfile(profileUpdates).await()

        // 2. Crear documento completo en users/{uid} en Firestore de inmediato
        val now = System.currentTimeMillis()
        val userDoc = hashMapOf(
            "uid" to user.uid,
            "name" to cleanName,
            "email" to cleanEmail,
            "phone" to cleanPhone,
            "dni" to "",
            "city" to "Lima, Perú",
            "role" to "cliente", // Seguridad: SIEMPRE cliente desde la app pública
            "photoUrl" to "",
            "preferences" to emptyList<String>(),
            "createdAtMillis" to now,
            "updatedAt" to now
        )

        firestore.collection("users")
            .document(user.uid)
            .set(userDoc, SetOptions.merge())
            .await()

        // 3. Enviar correo de verificación y desloguear por seguridad
        user.sendEmailVerification().await()
        auth.signOut()
    }

    /**
     * Inicia sesión validando que el correo esté verificado.
     */
    suspend fun login(email: String, pass: String): LoginResult {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), pass.trim()).await()
            val user = authResult.user ?: return LoginResult.Error("No se pudo obtener el usuario.")

            if (!user.isEmailVerified) {
                // Reenviar correo y expulsar
                runCatching { user.sendEmailVerification().await() }
                auth.signOut()
                LoginResult.EmailNotVerified
            } else {
                // Asegurar que el documento users/{uid} exista y tenga rol si fue creado por vías externas
                ensureUserDocumentExists(user)
                LoginResult.Success(user)
            }
        } catch (e: Exception) {
            LoginResult.Error(e.localizedMessage ?: "Error al iniciar sesión.")
        }
    }

    /**
     * Garantiza que el usuario tenga su documento con role = "cliente" en Firestore.
     */
    suspend fun ensureUserDocumentExists(user: FirebaseUser) {
        val docRef = firestore.collection("users").document(user.uid)
        val snapshot = docRef.get().await()

        if (!snapshot.exists() || snapshot.getString("role").isNullOrBlank()) {
            val parts = user.displayName.orEmpty().split("|").map { it.trim() }
            val name = parts.getOrNull(0)?.ifBlank { user.email?.substringBefore("@") } ?: "Turista"
            val phone = parts.getOrNull(2).orEmpty()
            val now = System.currentTimeMillis()

            val fallbackDoc = hashMapOf(
                "uid" to user.uid,
                "name" to name,
                "email" to (user.email ?: ""),
                "phone" to phone,
                "role" to "cliente",
                "city" to "Lima, Perú",
                "updatedAt" to now
            )
            docRef.set(fallbackDoc, SetOptions.merge()).await()
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    fun logout() {
        auth.signOut()
    }
}

sealed interface LoginResult {
    data class Success(val user: FirebaseUser) : LoginResult
    object EmailNotVerified : LoginResult
    data class Error(val message: String) : LoginResult
}