package com.example.perutours.data.repository

import com.example.perutours.data.model.Reservation
import com.example.perutours.data.model.TripDomainRules
import com.example.perutours.data.model.TripIncident
import com.example.perutours.data.model.TripItineraryItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface AgentTripDataSource {
    fun observeAssignedTrips(): Flow<List<Reservation>>
    fun observeItinerary(reservationId: String): Flow<List<TripItineraryItem>>
    fun observeIncidents(reservationId: String): Flow<List<TripIncident>>
    suspend fun getAssignedTrip(reservationId: String): Reservation
    suspend fun saveItineraryItem(item: TripItineraryItem)
    suspend fun deleteItineraryItem(itemId: String)
    suspend fun respondToIncident(incidentId: String, response: String, status: String)
}

class AgentTripRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AgentTripDataSource {

    override fun observeAssignedTrips(): Flow<List<Reservation>> = callbackFlow {
        val agentId = currentAgentId()
        val listener = firestore.collection(ReservationRepository.COLLECTION_RESERVATIONS)
            .whereEqualTo("agentId", agentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val trips = snapshot?.toObjects(Reservation::class.java)
                    .orEmpty()
                    .filter(TripDomainRules::isTripAvailable)
                    .sortedBy(Reservation::departureAtMillis)
                trySend(trips)
            }
        awaitClose(listener::remove)
    }

    override fun observeItinerary(
        reservationId: String
    ): Flow<List<TripItineraryItem>> = callbackFlow {
        val agentId = currentAgentId()
        val listener = firestore.collection(FirestoreTripRemoteDataSource.COLLECTION_ITINERARY)
            .whereEqualTo("reservationId", reservationId)
            .whereEqualTo("agentId", agentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(TripItineraryItem::class.java)
                    .orEmpty()
                    .sortedWith(
                        compareBy<TripItineraryItem> { it.dayIndex }
                            .thenBy { it.sortOrder }
                            .thenBy { it.startAtMillis }
                    )
                trySend(items)
            }
        awaitClose(listener::remove)
    }

    override fun observeIncidents(reservationId: String): Flow<List<TripIncident>> = callbackFlow {
        val agentId = currentAgentId()
        val listener = firestore.collection(FirestoreTripRemoteDataSource.COLLECTION_INCIDENTS)
            .whereEqualTo("reservationId", reservationId)
            .whereEqualTo("agentId", agentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val incidents = snapshot?.toObjects(TripIncident::class.java)
                    .orEmpty()
                    .sortedWith(
                        compareBy<TripIncident> { incidentStatusOrder(it.status) }
                            .thenByDescending { it.createdAtMillis }
                    )
                trySend(incidents)
            }
        awaitClose(listener::remove)
    }

    override suspend fun getAssignedTrip(reservationId: String): Reservation {
        require(reservationId.isNotBlank()) { "No se recibió la reserva." }
        val agentId = currentAgentId()
        val reservation = firestore.collection(ReservationRepository.COLLECTION_RESERVATIONS)
            .document(reservationId)
            .get()
            .await()
            .toObject(Reservation::class.java)
            ?: throw NoSuchElementException("La reserva ya no existe.")
        if (reservation.agentId != agentId) {
            throw IllegalAccessException("Esta reserva está asignada a otro agente.")
        }
        if (!TripDomainRules.isTripAvailable(reservation)) {
            throw IllegalStateException("El itinerario se habilita cuando el pago está aprobado.")
        }
        return reservation
    }

    override suspend fun saveItineraryItem(item: TripItineraryItem) {
        TripDomainRules.validateItineraryItem(item)?.let { throw IllegalArgumentException(it) }
        val reservation = getAssignedTrip(item.reservationId)
        require(item.agentId == reservation.agentId && item.clientId == reservation.clientId) {
            "La actividad no corresponde a los participantes de la reserva."
        }
        firestore.collection(FirestoreTripRemoteDataSource.COLLECTION_ITINERARY)
            .document(item.id)
            .set(item)
            .await()
    }

    override suspend fun deleteItineraryItem(itemId: String) {
        require(itemId.isNotBlank()) { "No se recibió la actividad." }
        val agentId = currentAgentId()
        val document = firestore.collection(FirestoreTripRemoteDataSource.COLLECTION_ITINERARY)
            .document(itemId)
        val item = document.get().await().toObject(TripItineraryItem::class.java)
            ?: return
        if (item.agentId != agentId) {
            throw IllegalAccessException("No puedes eliminar esta actividad.")
        }
        document.delete().await()
    }

    override suspend fun respondToIncident(
        incidentId: String,
        response: String,
        status: String
    ) {
        TripDomainRules.validateAgentResponse(response)?.let { throw IllegalArgumentException(it) }
        require(status in setOf(TripIncident.STATUS_IN_REVIEW, TripIncident.STATUS_RESOLVED)) {
            "Selecciona un estado válido para la atención."
        }
        val agentId = currentAgentId()
        val reference = firestore.collection(FirestoreTripRemoteDataSource.COLLECTION_INCIDENTS)
            .document(incidentId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(reference)
            val incident = snapshot.toObject(TripIncident::class.java)
                ?: throw NoSuchElementException("La incidencia ya no existe.")
            if (incident.agentId != agentId) {
                throw IllegalAccessException("La incidencia está asignada a otro agente.")
            }
            if (incident.status == TripIncident.STATUS_CLOSED) {
                throw IllegalStateException("Una incidencia cerrada no puede modificarse.")
            }
            val now = System.currentTimeMillis()
            transaction.update(
                reference,
                mapOf(
                    "agentResponse" to response.trim(),
                    "status" to status,
                    "updatedAtMillis" to now,
                    "resolvedAtMillis" to if (status == TripIncident.STATUS_RESOLVED) now else null
                )
            )
        }.await()
    }

    private fun currentAgentId(): String = auth.currentUser?.uid
        ?: throw IllegalStateException("Debes iniciar sesión como agente.")

    private fun incidentStatusOrder(status: String): Int = when (status) {
        TripIncident.STATUS_OPEN -> 0
        TripIncident.STATUS_IN_REVIEW -> 1
        TripIncident.STATUS_RESOLVED -> 2
        else -> 3
    }
}
