package com.example.perutours.service

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.perutours.data.model.Reservation
import java.util.concurrent.TimeUnit

interface TripBackgroundScheduler {
    fun enqueueChecklistSync()
    fun scheduleDepartureReminder(reservation: Reservation)
}

class TripWorkScheduler(
    private val workManager: WorkManager
) : TripBackgroundScheduler {

    override fun enqueueChecklistSync() {
        val request = OneTimeWorkRequestBuilder<TripSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            CHECKLIST_SYNC_WORK,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    override fun scheduleDepartureReminder(reservation: Reservation) {
        if (reservation.departureAtMillis <= System.currentTimeMillis()) return

        val triggerAt = reservation.departureAtMillis - REMINDER_ADVANCE_MILLIS
        val delay = (triggerAt - System.currentTimeMillis()).coerceAtLeast(0L)
        val inputData = Data.Builder()
            .putString(TripReminderWorker.KEY_RESERVATION_ID, reservation.id)
            .build()
        val request = OneTimeWorkRequestBuilder<TripReminderWorker>()
            .setInputData(inputData)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(
            "$REMINDER_WORK_PREFIX${reservation.id}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        private const val CHECKLIST_SYNC_WORK = "trip-checklist-sync"
        private const val REMINDER_WORK_PREFIX = "trip-departure-reminder-"
        private val REMINDER_ADVANCE_MILLIS = TimeUnit.HOURS.toMillis(24)
    }
}
