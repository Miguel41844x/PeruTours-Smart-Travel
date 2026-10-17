package com.example.perutours.data.messaging

import android.util.Log
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
        try {
            val user = auth.currentUser ?: return
            val token = refreshedToken ?: FirebaseMessaging.getInstance().token.await()

            val updateMap = hashMapOf<String, Any>(
                "fcmToken" to token,
                "updatedAt" to System.currentTimeMillis()
            )

            firestore
                .collection("users")
                .document(user.uid)
                .set(updateMap, SetOptions.merge())
                .await()
            Log.d("FCMTokenManager", "Token FCM guardado exitosamente para uid=${user.uid}")
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FCMTokenManager", "Error al guardar el token FCM", e)
        }
    }

    suspend fun clearToken() {
        try {
            val user = auth.currentUser ?: return
            val updateMap = hashMapOf<String, Any>(
                "fcmToken" to com.google.firebase.firestore.FieldValue.delete(),
                "updatedAt" to System.currentTimeMillis()
            )

            firestore
                .collection("users")
                .document(user.uid)
                .set(updateMap, SetOptions.merge())
                .await()

            runCatching { FirebaseMessaging.getInstance().deleteToken().await() }

            Log.d("FCMTokenManager", "Token FCM eliminado para uid=${user.uid}")
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("FCMTokenManager", "Error al eliminar el token FCM", e)
        }
    }
}