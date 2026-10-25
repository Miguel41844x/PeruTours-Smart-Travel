package com.example.perutours.data.repository

import com.example.perutours.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
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
    ): Result<Unit> = try {
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
        val userDoc = buildClientProfileDoc(
            uid = user.uid,
            name = cleanName,
            email = cleanEmail,
            phone = cleanPhone,
            nowMillis = System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(user.uid)
            .set(userDoc, SetOptions.merge())
            .await()

        // 3. Enviar correo de verificación y desloguear por seguridad
        user.sendEmailVerification().await()
        auth.signOut()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(AuthException(AuthErrorTranslator.translate(e, AuthOperation.REGISTRO), e))
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
            LoginResult.Error(AuthErrorTranslator.translate(e, AuthOperation.LOGIN), e)
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
            val derivedName = parts.getOrNull(0)?.ifBlank { user.email?.substringBefore("@") }
            // Las reglas exigen un nombre de 3 a 100 caracteres.
            val name = derivedName?.takeIf { it.length in 3..100 } ?: "Turista"
            val phone = parts.getOrNull(2).orEmpty()
            val now = System.currentTimeMillis()

            val fallbackDoc = buildClientProfileDoc(
                uid = user.uid,
                name = name,
                email = user.email ?: "",
                phone = phone,
                nowMillis = now
            )
            docRef.set(fallbackDoc, SetOptions.merge()).await()
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email.trim()).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(
            AuthException(AuthErrorTranslator.translate(e, AuthOperation.RESTABLECER_CONTRASENA), e)
        )
    }

    fun logout() {
        auth.signOut()
    }

    companion object {
        /**
         * Construye el documento completo de users/{uid} que exigen las reglas
         * de Firestore (isValidFullUser). El rol SIEMPRE es "cliente" desde la
         * app pública y el documento nunca incluye campos fuera de la lista
         * permitida (por ejemplo, createdAtMillis).
         */
        fun buildClientProfileDoc(
            uid: String,
            name: String,
            email: String,
            phone: String,
            nowMillis: Long
        ): Map<String, Any> = mapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "phone" to phone,
            "dni" to "",
            "city" to "Lima, Perú",
            "role" to "cliente", // Seguridad: SIEMPRE cliente desde la app pública
            "photoUrl" to "",
            "preferences" to emptyList<String>(),
            "updatedAt" to nowMillis
        )
    }
}

sealed interface LoginResult {
    data class Success(val user: FirebaseUser) : LoginResult
    object EmailNotVerified : LoginResult

    /**
     * @param message mensaje ya traducido al español y pensado para mostrarse en pantalla.
     * @param cause error original de Firebase, para los registros de error.
     */
    data class Error(val message: String, val cause: Throwable? = null) : LoginResult
}

/**
 * Error de autenticación cuyo mensaje ya fue traducido al español para la persona
 * que usa la app; conserva el error original en la causa.
 */
class AuthException(message: String, cause: Throwable) : Exception(message, cause)