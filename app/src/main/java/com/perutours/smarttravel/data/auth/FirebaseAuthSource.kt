package com.perutours.smarttravel.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAuthSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    val currentFirebaseUser: FirebaseUser? get() = firebaseAuth.currentUser

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    suspend fun login(email: String, password: String): FirebaseUser {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        return result.user ?: throw IllegalStateException("Inicio de sesión exitoso pero el usuario es nulo")
    }

    suspend fun register(email: String, password: String, firstName: String, lastName: String): FirebaseUser {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw IllegalStateException("Registro exitoso pero el usuario es nulo")

        val fullName = "$firstName $lastName".trim()
        val profileUpdates = userProfileChangeRequest {
            this.displayName = fullName
        }
        user.updateProfile(profileUpdates).await()

        val updatedUser = firebaseAuth.currentUser ?: user
        sendEmailVerificationInternal(updatedUser)
        return updatedUser
    }

    suspend fun sendEmailVerification() {
        val user = firebaseAuth.currentUser
            ?: throw IllegalStateException("No hay usuario autenticado")
        sendEmailVerificationInternal(user)
    }

    private suspend fun sendEmailVerificationInternal(user: FirebaseUser) {
        user.sendEmailVerification().await()
    }

    suspend fun reloadUser(): FirebaseUser {
        val user = firebaseAuth.currentUser
            ?: throw IllegalStateException("No hay usuario autenticado")
        user.reload().await()
        return firebaseAuth.currentUser ?: user
    }

    suspend fun logout() {
        firebaseAuth.signOut()
    }
}
