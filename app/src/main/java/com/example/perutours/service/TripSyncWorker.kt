package com.example.perutours.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.perutours.data.local.TripChecklistEntity
import com.example.perutours.data.local.TripDao
import com.example.perutours.data.local.toModel
import com.example.perutours.data.repository.FirestoreTripRemoteDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Envía a Firestore los cambios que el viajero hizo sin conexión. */
class TripSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val auth: FirebaseAuth by inject()
    private val firestore: FirebaseFirestore by inject()
    private val tripDao: TripDao by inject()

    override suspend fun doWork(): Result {
        val clientId = auth.currentUser?.uid ?: return Result.failure()
        val pending = tripDao.getPendingChecklist(clientId)
        if (pending.isEmpty()) return Result.success()

        return try {
            pending.forEach { item -> syncItem(item, clientId) }
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    private suspend fun syncItem(item: TripChecklistEntity, clientId: String) {
        if (item.clientId != clientId) return

        val reservation = firestore.collection("reservations")
            .document(item.reservationId)
            .get()
            .await()
        if (!reservation.exists() || reservation.getString("clientId") != clientId) {
            throw IllegalAccessException("El checklist no pertenece al usuario autenticado.")
        }

        val document = firestore.collection(FirestoreTripRemoteDataSource.COLLECTION_CHECKLIST)
            .document(item.id)
        when (item.syncState) {
            TripChecklistEntity.PENDING_DELETE -> {
                document.delete().await()
                tripDao.deleteChecklistPermanently(item.id)
            }
            TripChecklistEntity.PENDING_UPSERT -> {
                document.set(item.toModel()).await()
                tripDao.markChecklistSynced(item.id)
            }
        }
    }

    companion object {
        private const val MAX_RETRIES = 4
    }
}
