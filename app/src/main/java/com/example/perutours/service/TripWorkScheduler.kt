package com.example.perutours.service

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.perutours.data.model.Reservation
import java.util.concurrent.TimeUnit

interface TripBackgroundScheduler {
    fun enqueueChecklistSync()
    fun scheduleDepartureReminder(reservation: Reservation)
    fun enqueueReminderRefresh(clientId: String) {}
    fun cancelRemindersForUser(clientId: String) {}
}

class TripWorkScheduler(
    private val workManager: WorkManager,
    private val reminderStore: TripReminderStore
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
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    override fun scheduleDepartureReminder(reservation: Reservation) {
        if (reservation.id.isBlank() || reservation.clientId.isBlank()) return
        val reservationTag = REMINDER_WORK_PREFIX + reservation.clientId + "-" + reservation.id
        val workName = reservationTag + "-" + reservation.departureAtMillis
        if (reservation.status != Reservation.STATUS_PAID ||
            reservation.departureAtMillis <= System.currentTimeMillis()) {
            workManager.cancelAllWorkByTag(reservationTag)
            return
        }
        if (reminderStore.wasDelivered(reservation.clientId, reservation.id, reservation.departureAtMillis)) return

        val triggerAt = reservation.departureAtMillis - REMINDER_ADVANCE_MILLIS
        val delay = (triggerAt - System.currentTimeMillis()).coerceAtLeast(0L)
        val inputData = Data.Builder()
            .putString(TripReminderWorker.KEY_RESERVATION_ID, reservation.id)
            .putString(TripReminderWorker.KEY_CLIENT_ID, reservation.clientId)
            .putLong(TripReminderWorker.KEY_DEPARTURE, reservation.departureAtMillis)
            .build()
        val request = OneTimeWorkRequestBuilder<TripReminderWorker>()
            .setInputData(inputData)
            .addTag(ownerTag(reservation.clientId))
            .addTag(reservationTag)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    override fun enqueueReminderRefresh(clientId: String) {
        val request = PeriodicWorkRequestBuilder<TripReminderRefreshWorker>(1, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(Data.Builder().putString(TripReminderWorker.KEY_CLIENT_ID, clientId).build())
            .addTag(ownerTag(clientId)).build()
        workManager.enqueueUniquePeriodicWork("trip-reminder-refresh-" + clientId,
            ExistingPeriodicWorkPolicy.KEEP, request)
    }

    override fun cancelRemindersForUser(clientId: String) {
        workManager.cancelAllWorkByTag(ownerTag(clientId))
    }

    private fun ownerTag(clientId: String) = "trip-reminders-owner-" + clientId

    companion object {
        private const val CHECKLIST_SYNC_WORK = "trip-checklist-sync"
        private const val REMINDER_WORK_PREFIX = "trip-departure-reminder-"
        private val REMINDER_ADVANCE_MILLIS = TimeUnit.HOURS.toMillis(24)
    }
}
