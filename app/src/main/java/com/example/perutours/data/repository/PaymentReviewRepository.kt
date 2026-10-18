package com.example.perutours.data.repository

import com.example.perutours.data.model.Payment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * HU07: acceso de lectura al pago independiente (payments/{paymentId}).
 * El ID del pago es el ID de la reserva, por lo que se consulta por la
 * reserva. La revisión del agente se agrega en este mismo repositorio.
 */
interface PaymentReviewDataSource {
    suspend fun getPaymentForReservation(reservationId: String): Payment?
}

class PaymentReviewRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : PaymentReviewDataSource {

    override suspend fun getPaymentForReservation(reservationId: String): Payment? {
        if (reservationId.isBlank()) return null
        val snapshot = firestore.collection(COLLECTION_PAYMENTS)
            .document(reservationId)
            .get()
            .await()
        if (!snapshot.exists()) return null
        return snapshot.toObject(Payment::class.java)
    }

    companion object {
        const val COLLECTION_PAYMENTS = "payments"
    }
}
