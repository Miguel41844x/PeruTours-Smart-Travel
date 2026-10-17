package com.example.perutours

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.perutours.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PeruToursApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@PeruToursApplication)
            modules(appModule)
        }

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "perutours_fcm_quotations"
            val channelName = "Cotizaciones PeruTours"
            val channelDescription = "Notificaciones de nuevas cotizaciones de viaje listas"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}