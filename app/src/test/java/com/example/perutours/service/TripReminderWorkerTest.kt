package com.example.perutours.service

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.perutours.data.local.TripDao
import com.example.perutours.data.local.toTripEntity
import com.example.perutours.data.model.Reservation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TripReminderWorkerTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val auth = mockk<FirebaseAuth>()
    private val user = mockk<FirebaseUser>()
    private val dao = mockk<TripDao>()
    private val scheduler = mockk<TripBackgroundScheduler>(relaxed = true)
    private lateinit var store: TripReminderStore
    private val booking = Reservation(id = "res", clientId = "client", destination = "Cusco",
        status = Reservation.STATUS_PAID, departureAtMillis = System.currentTimeMillis() + 3_600_000L)

    @Before fun setup() {
        stopKoin()
        every { auth.currentUser } returns user
        every { user.uid } returns "client"
        every { user.isEmailVerified } returns true
        coEvery { dao.getTrip("res") } returns booking.toTripEntity()
        context.getSharedPreferences("trip_reminder_delivery", Context.MODE_PRIVATE).edit().clear().commit()
        store = TripReminderStore(context)
        startKoin { modules(module {
            single { auth }; single { dao }; single { store }; single { scheduler }
        }) }
        Shadows.shadowOf(context as Application).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager().createNotificationChannel(
            NotificationChannel(PeruToursApplicationChannels.TRIP_REMINDERS, "Viajes", NotificationManager.IMPORTANCE_HIGH))
        manager().cancelAll()
    }

    @After fun teardown() { stopKoin(); manager().cancelAll() }

    @Test fun paidTripCreatesOneNotificationAndDeliverySurvivesReopeningTheStore() = runBlocking {
        worker().doWork()
        assertEquals(1, manager().activeNotifications.size)
        assertTrue(TripReminderStore(context).wasDelivered("client", "res", booking.departureAtMillis))
        var repeated = 0
        store.deliverOnce("client", "res", booking.departureAtMillis) { repeated++ }
        assertEquals(0, repeated)
    }

    @Test fun anotherSessionNeverReceivesThePreviousUsersReminder() = runBlocking {
        every { user.uid } returns "other-client"
        worker().doWork()
        assertTrue(manager().activeNotifications.isEmpty())
    }

    @Test fun expiredAndUnpaidTripsDoNotNotify() = runBlocking {
        coEvery { dao.getTrip("res") } returns booking.copy(status = Reservation.STATUS_CONFIRMED).toTripEntity()
        worker().doWork()
        coEvery { dao.getTrip("res") } returns booking.copy(departureAtMillis = System.currentTimeMillis() - 1).toTripEntity()
        worker().doWork()
        assertTrue(manager().activeNotifications.isEmpty())
    }

    @Test fun aChangedDepartureReschedulesInsteadOfSendingAnOldReminder() = runBlocking {
        val moved = booking.copy(departureAtMillis = booking.departureAtMillis + 86_400_000)
        coEvery { dao.getTrip("res") } returns moved.toTripEntity()
        worker().doWork()
        verify(exactly = 1) { scheduler.scheduleDepartureReminder(match { it.departureAtMillis == moved.departureAtMillis }) }
        assertTrue(manager().activeNotifications.isEmpty())
    }

    private fun manager() = context.getSystemService(NotificationManager::class.java)
    private fun worker() = TestListenableWorkerBuilder<TripReminderWorker>(
        context, inputData = Data.Builder().putString(TripReminderWorker.KEY_RESERVATION_ID, "res")
            .putString(TripReminderWorker.KEY_CLIENT_ID, "client")
            .putLong(TripReminderWorker.KEY_DEPARTURE, booking.departureAtMillis).build()
    ).build()
}
