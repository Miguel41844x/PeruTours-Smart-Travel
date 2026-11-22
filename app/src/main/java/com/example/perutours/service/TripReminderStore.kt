package com.example.perutours.service

import android.content.Context

/** No repetir el aviso de la misma salida al refrescar o reiniciar la app. */
class TripReminderStore(context: Context) {
    private val preferences = context.getSharedPreferences("trip_reminder_delivery", Context.MODE_PRIVATE)
    private fun key(owner: String, reservation: String, departure: Long) = owner + ":" + reservation + ":" + departure
    fun wasDelivered(owner: String, reservation: String, departure: Long) =
        preferences.getBoolean(key(owner, reservation, departure), false)

    fun deliverOnce(owner: String, reservation: String, departure: Long, deliver: () -> Unit) {
        synchronized(lock) {
            if (wasDelivered(owner, reservation, departure)) return
            deliver()
            preferences.edit().putBoolean(key(owner, reservation, departure), true).commit()
        }
    }

    companion object { private val lock = Any() }
}
