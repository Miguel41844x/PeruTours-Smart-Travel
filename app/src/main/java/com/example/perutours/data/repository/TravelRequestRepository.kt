package com.example.perutours.data.repository

import com.example.perutours.data.model.TravelRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TravelRequestRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun save(request: TravelRequest): TravelRequest {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("No hay un usuario autenticado.")
        val document = firestore.collection(COLLECTION_NAME).document()
        val savedRequest = request.copy(
            id = document.id,
            userId = uid,
            status = TravelRequest.STATUS_PENDING_QUOTE,
            createdAtMillis = System.currentTimeMillis()
        )

        document.set(savedRequest).await()
        return savedRequest
    }

    companion object {
        const val COLLECTION_NAME = "travel_requests"
    }
}
