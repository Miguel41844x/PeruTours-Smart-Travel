package com.example.perutours.data.repository

import com.example.perutours.data.model.Reservation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface ReservationDataSource {
    suspend fun getCurrentClientReservations(): List<Reservation>
    suspend fun getCurrentClientReservationById(reservationId: String): Reservation?
    fun observeCurrentClientReservations(): Flow<List<Reservation>>
}

class ReservationRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ReservationDataSource {

    override suspend fun getCurrentClientReservations(): List<Reservation> {
        val clientId = currentClientId()
        val snapshot = firestore.collection(COLLECTION_RESERVATIONS)
            .whereEqualTo("clientId", clientId)
            .get()
            .await()

        return snapshot.toObjects(Reservation::class.java)
            .sortedByDescending { reservation -> reservation.createdAtMillis }
    }

    override suspend fun getCurrentClientReservationById(
        reservationId: String
    ): Reservation? {
        require(reservationId.isNotBlank()) {
            "No se recibió el identificador de la reserva."
        }

        val clientId = currentClientId()
        val reservation = firestore.collection(COLLECTION_RESERVATIONS)
            .document(reservationId)
            .get()
            .await()
            .toObject(Reservation::class.java)
            ?: return null

        if (reservation.clientId != clientId) {
            throw IllegalAccessException("Esta reserva no pertenece al usuario autenticado.")
        }
        return reservation
    }

    override fun observeCurrentClientReservations(): Flow<List<Reservation>> = callbackFlow {
        val clientId = auth.currentUser?.uid
        if (clientId == null) {
            close(IllegalStateException("Debes iniciar sesión para consultar tus reservas."))
            return@callbackFlow
        }

        val listener = firestore.collection(COLLECTION_RESERVATIONS)
            .whereEqualTo("clientId", clientId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val reservations = snapshot
                    ?.toObjects(Reservation::class.java)
                    .orEmpty()
                    .sortedByDescending { reservation -> reservation.createdAtMillis }
                trySend(reservations)
            }

        awaitClose { listener.remove() }
    }

    private fun currentClientId(): String = auth.currentUser?.uid
        ?: throw IllegalStateException("Debes iniciar sesión para consultar tus reservas.")

    companion object {
        const val COLLECTION_RESERVATIONS = "reservations"
    }
}
