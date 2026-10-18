package com.example.perutours.data.repository

import com.example.perutours.data.model.Payment
import com.example.perutours.data.model.Reservation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * HU07: acceso al pago independiente (payments/{paymentId}).
 *
 * El cliente lee su propio pago; el agente asignado lista los
 * pendientes y los resuelve (aprobar o rechazar) actualizando el pago
 * y su reserva en la misma tanda para que las reglas de Firestore
 * validen el cruce de forma atómica.
 */
interface PaymentReviewDataSource {
    suspend fun getPaymentForReservation(reservationId: String): Payment?

    /** Pagos 'Pendiente' asignados al agente autenticado. */
    suspend fun getPendingPaymentsForAgent(): List<Payment>

    /** Reserva asociada al pago, visible solo para el agente asignado. */
    suspend fun getReservationForAgent(reservationId: String): Reservation?

    /** Nombre del cliente dueño del pago (para mostrar en el panel). */
    suspend fun getClientName(clientId: String): String?

    /** Aprueba el pago: pago 'Aprobado' + reserva 'Pagado'. */
    suspend fun approvePayment(payment: Payment)

    /** Rechaza el pago con motivo >= 10 caracteres + reserva 'Pago rechazado'. */
    suspend fun rejectPayment(payment: Payment, rejectionReason: String)
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

    override suspend fun getPendingPaymentsForAgent(): List<Payment> {
        val agentId = currentAgentId() ?: return emptyList()
        val snapshot = firestore.collection(COLLECTION_PAYMENTS)
            .whereEqualTo("agentId", agentId)
            .whereEqualTo("status", Payment.STATUS_PENDING)
            .get()
            .await()
        return snapshot.toObjects(Payment::class.java)
            .sortedByDescending { payment -> payment.submittedAtMillis }
    }

    override suspend fun getReservationForAgent(reservationId: String): Reservation? {
        if (reservationId.isBlank()) return null
        val snapshot = firestore.collection(ReservationRepository.COLLECTION_RESERVATIONS)
            .document(reservationId)
            .get()
            .await()
        if (!snapshot.exists()) return null
        return snapshot.toObject(Reservation::class.java)
    }

    override suspend fun getClientName(clientId: String): String? {
        if (clientId.isBlank()) return null
        val snapshot = firestore.collection(COLLECTION_USERS)
            .document(clientId)
            .get()
            .await()
        if (!snapshot.exists()) return null
        return snapshot.getString("name")
    }

    override suspend fun approvePayment(payment: Payment) {
        val agentId = currentAgentId()
            ?: throw IllegalStateException("La sesión terminó. Ingresa nuevamente.")
        require(PaymentValidator.canAgentReview(payment.status)) {
            "Este pago ya fue resuelto."
        }

        val now = System.currentTimeMillis()
        val paymentRef = firestore.collection(COLLECTION_PAYMENTS)
            .document(payment.id)
        val reservationRef = firestore.collection(ReservationRepository.COLLECTION_RESERVATIONS)
            .document(payment.reservationId)

        val batch = firestore.batch()
        batch.update(
            paymentRef,
            mapOf(
                "status" to Payment.STATUS_APPROVED,
                "rejectionReason" to "",
                "reviewedAtMillis" to now,
                "reviewedBy" to agentId
            )
        )
        batch.update(
            reservationRef,
            mapOf("status" to Reservation.STATUS_PAID)
        )
        batch.commit().await()
    }

    override suspend fun rejectPayment(payment: Payment, rejectionReason: String) {
        val agentId = currentAgentId()
            ?: throw IllegalStateException("La sesión terminó. Ingresa nuevamente.")
        require(PaymentValidator.isValidRejectionReason(rejectionReason)) {
            "El motivo del rechazo debe tener al menos ${PaymentValidator.MIN_REJECTION_LENGTH} caracteres."
        }
        require(PaymentValidator.canAgentReview(payment.status)) {
            "Este pago ya fue resuelto."
        }

        val now = System.currentTimeMillis()
        val paymentRef = firestore.collection(COLLECTION_PAYMENTS)
            .document(payment.id)
        val reservationRef = firestore.collection(ReservationRepository.COLLECTION_RESERVATIONS)
            .document(payment.reservationId)

        val batch = firestore.batch()
        batch.update(
            paymentRef,
            mapOf(
                "status" to Payment.STATUS_REJECTED,
                "rejectionReason" to rejectionReason.trim(),
                "reviewedAtMillis" to now,
                "reviewedBy" to agentId
            )
        )
        batch.update(
            reservationRef,
            mapOf("status" to Reservation.STATUS_PAYMENT_REJECTED)
        )
        batch.commit().await()
    }

    private fun currentAgentId(): String? = auth.currentUser?.uid

    companion object {
        const val COLLECTION_PAYMENTS = "payments"
        const val COLLECTION_USERS = "users"
    }
}
