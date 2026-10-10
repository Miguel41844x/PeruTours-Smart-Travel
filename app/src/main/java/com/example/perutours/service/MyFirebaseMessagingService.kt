package com.example.perutours.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.perutours.MainActivity
import com.example.perutours.data.messaging.FCMTokenManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { FCMTokenManager().saveToken(token) }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "¡Tu cotización de viaje está lista! ✈️"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "El agente turístico ha preparado tu propuesta con estado 'Cotizado'."

        val quotationId = remoteMessage.data["quotationId"] ?: ""
        val notificationType = remoteMessage.data["type"] ?: TYPE_QUOTATION

        mostrarNotificacionPush(title, body, quotationId, notificationType)
    }

    private fun mostrarNotificacionPush(
        title: String,
        body: String,
        quotationId: String,
        notificationType: String
    ) {
        val channelId = "perutours_fcm_quotations"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Cotizaciones PeruTours",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de nuevas cotizaciones de viaje listas"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_QUOTATION_ID, quotationId)
            putExtra(EXTRA_NOTIFICATION_TYPE, notificationType)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            quotationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (canNotify) {
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        }
    }

    companion object {
        const val EXTRA_QUOTATION_ID = "EXTRA_QUOTATION_ID"
        const val EXTRA_NOTIFICATION_TYPE = "EXTRA_NOTIFICATION_TYPE"
        const val TYPE_QUOTATION = "quotation"
        const val TYPE_QUOTATION_RESPONSE = "quotation_response"
    }
}
