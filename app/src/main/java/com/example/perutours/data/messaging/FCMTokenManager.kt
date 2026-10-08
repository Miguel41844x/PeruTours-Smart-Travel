package com.example.perutours.data.messaging

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class FCMTokenManager(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun saveToken() {

        val user = auth.currentUser
            ?: return

        val token = FirebaseMessaging
            .getInstance()
            .token
            .await()

        firestore
            .collection("users")
            .document(user.uid)
            .update("fcmToken", token)
            .await()
    }
}