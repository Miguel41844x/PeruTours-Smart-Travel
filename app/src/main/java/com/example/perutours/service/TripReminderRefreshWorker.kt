package com.example.perutours.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.perutours.data.local.TripDao
import com.example.perutours.data.local.toTripEntity
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.repository.ReservationDataSource
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Actualiza las salidas pagadas con red disponible incluso tras cerrar la app. */
class TripReminderRefreshWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {
    private val auth: FirebaseAuth by inject()
    private val repository: ReservationDataSource by inject()
    private val dao: TripDao by inject()
    private val scheduler: TripBackgroundScheduler by inject()

    override suspend fun doWork(): Result {
        val uid = inputData.getString(TripReminderWorker.KEY_CLIENT_ID) ?: return Result.failure()
        if (auth.currentUser?.uid != uid || auth.currentUser?.isEmailVerified != true) return Result.success()
        return try {
            val reservations = repository.getCurrentClientReservations().filter { it.clientId == uid }
            if (auth.currentUser?.uid != uid) return Result.success()
            dao.upsertTrips(reservations.filter { it.status == Reservation.STATUS_PAID }.map { it.toTripEntity() })
            reservations.forEach(scheduler::scheduleDepartureReminder)
            Result.success()
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            if (runAttemptCount < 4) Result.retry() else Result.failure()
        }
    }
}
