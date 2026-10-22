package com.example.perutours.data.repository

import com.example.perutours.data.local.TripChecklistEntity
import com.example.perutours.data.local.TripDao
import com.example.perutours.data.local.toEntity
import com.example.perutours.data.local.toModel
import com.example.perutours.data.local.toReservation
import com.example.perutours.data.local.toTripEntity
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.model.ReservationItineraryBuilder
import com.example.perutours.data.model.TripDomainRules
import com.example.perutours.data.model.TripIncident
import com.example.perutours.data.model.TripItineraryItem
import com.example.perutours.data.model.TripOverview
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

interface TripRemoteDataSource {
    fun currentUserId(): String
    suspend fun getPaidReservations(): List<Reservation>
    suspend fun getPaidReservation(reservationId: String): Reservation?
    suspend fun getItinerary(reservationId: String): List<TripItineraryItem>
    suspend fun getChecklist(reservationId: String): List<com.example.perutours.data.model.TripChecklistItem>
    suspend fun getIncidents(reservationId: String): List<TripIncident>
}

class FirestoreTripRemoteDataSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : TripRemoteDataSource {

    override fun currentUserId(): String = auth.currentUser?.uid
        ?: throw IllegalStateException("Debes iniciar sesión para consultar tus viajes.")

    override suspend fun getPaidReservations(): List<Reservation> {
        val clientId = currentUserId()
        return firestore.collection(ReservationRepository.COLLECTION_RESERVATIONS)
            .whereEqualTo("clientId", clientId)
            .get()
            .await()
            .toObjects(Reservation::class.java)
            .filter(TripDomainRules::isTripAvailable)
            .sortedBy(Reservation::departureAtMillis)
    }

    override suspend fun getPaidReservation(reservationId: String): Reservation? {
        require(reservationId.isNotBlank()) { "No se recibió el identificador de la reserva." }
        val clientId = currentUserId()
        val reservation = firestore.collection(ReservationRepository.COLLECTION_RESERVATIONS)
            .document(reservationId)
            .get()
            .await()
            .toObject(Reservation::class.java)
            ?: return null

        if (reservation.clientId != clientId) {
            throw IllegalAccessException("Esta reserva no pertenece al usuario autenticado.")
        }
        return reservation.takeIf(TripDomainRules::isTripAvailable)
    }

    override suspend fun getItinerary(reservationId: String): List<TripItineraryItem> {
        val clientId = currentUserId()
        return firestore.collection(COLLECTION_ITINERARY)
            .whereEqualTo("reservationId", reservationId)
            .whereEqualTo("clientId", clientId)
            .get()
            .await()
            .toObjects(TripItineraryItem::class.java)
            .sortedWith(
                compareBy<TripItineraryItem> { it.dayIndex }
                    .thenBy { it.sortOrder }
                    .thenBy { it.startAtMillis }
            )
    }

    override suspend fun getChecklist(
        reservationId: String
    ): List<com.example.perutours.data.model.TripChecklistItem> {
        val clientId = currentUserId()
        return firestore.collection(COLLECTION_CHECKLIST)
            .whereEqualTo("reservationId", reservationId)
            .whereEqualTo("clientId", clientId)
            .get()
            .await()
            .toObjects(com.example.perutours.data.model.TripChecklistItem::class.java)
            .sortedWith(
                compareBy<com.example.perutours.data.model.TripChecklistItem> { it.isCompleted }
                    .thenBy { it.createdAtMillis }
            )
    }

    override suspend fun getIncidents(reservationId: String): List<TripIncident> {
        val clientId = currentUserId()
        return firestore.collection(COLLECTION_INCIDENTS)
            .whereEqualTo("reservationId", reservationId)
            .whereEqualTo("clientId", clientId)
            .get()
            .await()
            .toObjects(TripIncident::class.java)
            .sortedByDescending(TripIncident::createdAtMillis)
    }

    companion object {
        const val COLLECTION_ITINERARY = "trip_itinerary_items"
        const val COLLECTION_CHECKLIST = "trip_checklist_items"
        const val COLLECTION_INCIDENTS = "trip_incidents"
    }
}

interface TripDataSource {
    fun observeTrips(): Flow<List<Reservation>>
    fun observeTrip(reservationId: String): Flow<TripOverview?>
    suspend fun refreshTrips(): Result<Unit>
    suspend fun refreshTrip(reservationId: String): Result<Unit>
}

class TripRepository(
    private val remote: TripRemoteDataSource,
    private val tripDao: TripDao
) : TripDataSource {

    override fun observeTrips(): Flow<List<Reservation>> {
        val clientId = remote.currentUserId()
        return tripDao.observeTrips(clientId).map { trips ->
            trips.map { it.toReservation() }
        }
    }

    override fun observeTrip(reservationId: String): Flow<TripOverview?> = combine(
        tripDao.observeTrip(reservationId),
        tripDao.observeItinerary(reservationId),
        tripDao.observeChecklist(reservationId),
        tripDao.observeIncidents(reservationId)
    ) { trip, itinerary, checklist, incidents ->
        trip?.let {
            TripOverview(
                reservation = it.toReservation(),
                itinerary = itinerary.map { entity -> entity.toModel() },
                checklist = checklist.map { entity -> entity.toModel() },
                incidents = incidents.map { entity -> entity.toModel() }
            )
        }
    }

    override suspend fun refreshTrips(): Result<Unit> = runCatching {
        val clientId = remote.currentUserId()
        val reservations = remote.getPaidReservations()
        tripDao.replaceTrips(
            clientId = clientId,
            trips = reservations.map { it.toTripEntity() }
        )
    }

    override suspend fun refreshTrip(reservationId: String): Result<Unit> = runCatching {
        require(reservationId.isNotBlank()) { "No se recibió el identificador del viaje." }
        val reservation = remote.getPaidReservation(reservationId)
            ?: throw NoSuchElementException("El viaje no existe o todavía no está pagado.")

        tripDao.upsertTrips(listOf(reservation.toTripEntity()))

        val remoteItinerary = remote.getItinerary(reservationId)
        val itinerary = remoteItinerary.ifEmpty {
            ReservationItineraryBuilder.build(reservation)
        }
        tripDao.replaceItinerary(
            reservationId = reservationId,
            items = itinerary.map(TripItineraryItem::toEntity)
        )

        val pendingChecklistIds = tripDao.getPendingChecklist(reservation.clientId)
            .asSequence()
            .filter { it.reservationId == reservationId }
            .map(TripChecklistEntity::id)
            .toSet()
        val remoteChecklist = remote.getChecklist(reservationId)
            .filterNot { it.id in pendingChecklistIds }
            .map { it.toEntity() }
        tripDao.mergeRemoteChecklist(reservationId, remoteChecklist)

        tripDao.replaceIncidents(
            reservationId = reservationId,
            incidents = remote.getIncidents(reservationId).map(TripIncident::toEntity)
        )
    }
}
