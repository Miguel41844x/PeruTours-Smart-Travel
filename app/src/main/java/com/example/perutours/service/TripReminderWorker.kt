package com.example.perutours.service

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.perutours.MainActivity
import com.example.perutours.R
import com.example.perutours.data.local.TripDao
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TripReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val tripDao: TripDao by inject()

    override suspend fun doWork(): Result {
        val reservationId = inputData.getString(KEY_RESERVATION_ID).orEmpty()
        if (reservationId.isBlank()) return Result.failure()
        val trip = tripDao.getTrip(reservationId) ?: return Result.success()

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            reservationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(
            applicationContext,
            PeruToursApplicationChannels.TRIP_REMINDERS
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Tu viaje a ${trip.destination} está cerca")
            .setContentText("Revisa el itinerario y completa tu lista antes de salir.")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        applicationContext.getSystemService(NotificationManager::class.java)
            ?.notify(reservationId.hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val KEY_RESERVATION_ID = "reservation_id"
    }
}

object PeruToursApplicationChannels {
    const val TRIP_REMINDERS = "perutours_trip_reminders"
}
