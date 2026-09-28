package com.example.perutours.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest

object AuthManager {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private const val TAG = "AuthManager"

    fun getAuthInstance(): FirebaseAuth = auth

    fun register(
        name: String,
        email: String,
        password: String,
        role: String,
        callback: (Boolean, String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    // Guardamos el nombre y el rol en el perfil
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName("$name ($role)")
                        .build()
                    user?.updateProfile(profileUpdates)

                    callback(true, "Cuenta creada exitosamente para $name.")
                } else {
                    callback(false, task.exception?.message ?: "Error al registrar.")
                }
            }
    }

    fun login(email: String, password: String, callback: (Boolean, String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Login correcto: ${task.result}")
                    callback(true, "Inicio de sesión exitoso.")
                } else {
                    callback(false, task.exception?.message ?: "Error al iniciar sesión.")
                }
            }
    }

    fun signInWithGoogle(idToken: String, callback: (Boolean, String) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    callback(true, "Inicio de sesión con Google exitoso: ${user?.email}")
                } else {
                    val errorMsg = task.exception?.message ?: "Error al iniciar con Google."
                    callback(false, errorMsg)
                }
            }
    }
}