package com.example.perutours.data.messaging

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class FCMTokenManager(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun saveToken(refreshedToken: String? = null) {
        val user = auth.currentUser ?: return
        val token = refreshedToken ?: FirebaseMessaging.getInstance().token.await()

        // Guardamos el token preservando o inicializando campos básicos para no romper reglas
        val updateMap = hashMapOf<String, Any>(
            "fcmToken" to token,
            "updatedAt" to System.currentTimeMillis()
        )

        firestore
            .collection("users")
            .document(user.uid)
            .set(updateMap, SetOptions.merge())
            .await()
    }
}