package com.example.perutours.service

import android.util.Log
import com.example.perutours.data.local.TripDao
import com.example.perutours.data.local.toReservation
import com.example.perutours.data.local.toTripEntity
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.repository.ReservationDataSource
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.retryWhen

/** HU14: escucha las reservas de la sesión, aunque no se abra Mis viajes. */
class TripReminderCoordinator(
    private val auth: FirebaseAuth,
    private val reservations: ReservationDataSource,
    private val dao: TripDao,
    private val scheduler: TripBackgroundScheduler
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sessionJob: Job? = null
    private var owner: String? = null
    private var started = false

    fun start() {
        if (started) return
        started = true
        auth.addAuthStateListener { session ->
            val uid = session.currentUser?.takeIf { it.isEmailVerified }?.uid
            if (owner != uid) owner?.let(scheduler::cancelRemindersForUser)
            sessionJob?.cancel()
            owner = uid
            if (uid == null) return@addAuthStateListener
            scheduler.enqueueReminderRefresh(uid)
            rescheduleCached()
            sessionJob = scope.launch {
                try {
                    reservations.observeCurrentClientReservations().retryWhen { _, attempt ->
                    if (attempt >= 4) return@retryWhen false
                    delay(((attempt + 1) * 2_000).coerceAtMost(30_000))
                    auth.currentUser?.uid == uid
                }.collect { values ->
                    if (auth.currentUser?.uid != uid) return@collect
                    val owned = values.filter { it.clientId == uid }
                    val paid = owned.filter { it.status == Reservation.STATUS_PAID }
                    dao.upsertTrips(paid.map { it.toTripEntity() })
                    if (auth.currentUser?.uid != uid) return@collect
                    owned.forEach(scheduler::scheduleDepartureReminder)
                }
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    Log.w("TripReminders", "No se pudieron actualizar los recordatorios; se reintentará con la sincronización.")
                }
            }
        }
    }

    fun rescheduleCached() {
        val uid = auth.currentUser?.takeIf { it.isEmailVerified }?.uid ?: return
        scope.launch {
            try {
                val cached = dao.observeTrips(uid).first()
                if (auth.currentUser?.uid == uid) cached.forEach {
                    scheduler.scheduleDepartureReminder(it.toReservation())
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.w("TripReminders", "No se pudo reprogramar la caché local.")
            }
        }
    }
}
