package com.example.perutours.service

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.perutours.data.model.Reservation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.tasks.await
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class UploadPaymentWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val storage: FirebaseStorage by inject()
    private val firestore: FirebaseFirestore by inject()
    private val auth: FirebaseAuth by inject()

    companion object {
        const val KEY_QUOTATION_ID = "quotation_id"
        const val KEY_RESERVATION_ID = "reservation_id"
        const val KEY_IMAGE_PATH = "image_path"
        const val KEY_PAYMENT_METHOD = "payment_method"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_ERROR = "error"
    }

    override suspend fun doWork(): Result {
        val quotationId = inputData.getString(KEY_QUOTATION_ID).orEmpty()
        val reservationId = inputData.getString(KEY_RESERVATION_ID).orEmpty()
        val imagePath = inputData.getString(KEY_IMAGE_PATH).orEmpty()
        val paymentMethod = inputData.getString(KEY_PAYMENT_METHOD) ?: "Transferencia / Yape"

        if (quotationId.isBlank() || reservationId.isBlank() || imagePath.isBlank()) {
            return failure("Los datos del pago están incompletos")
        }

        val file = File(imagePath)
        if (!file.exists()) {
            return failure("El comprobante no existe en el almacenamiento local")
        }

        return try {
            val userId = auth.currentUser?.uid
                ?: return failure("La sesión terminó antes de subir el comprobante")
            val reservationRef = firestore.collection("reservations").document(reservationId)
            val reservation = reservationRef.get().await()

            if (!reservation.exists() || reservation.getString("clientId") != userId) {
                return failure("La reserva no pertenece al usuario autenticado")
            }
            if (reservation.getString("quotationId") != quotationId) {
                return failure("La cotización no corresponde a la reserva")
            }
            if (reservation.getString("status") == Reservation.STATUS_PAID) {
                file.delete()
                return Result.success(
                    workDataOf(KEY_DOWNLOAD_URL to reservation.getString("paymentVoucherUrl").orEmpty())
                )
            }
            if (reservation.getString("status") != Reservation.STATUS_CONFIRMED) {
                return failure("La reserva no está disponible para registrar el pago")
            }

            val storageRef = storage.reference.child(
                "payment_vouchers/$userId/$reservationId/${file.name}"
            )

            val downloadUrl = try {
                storageRef.metadata.await()
                storageRef.downloadUrl.await().toString()
            } catch (error: StorageException) {
                if (error.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw error
                storageRef.putFile(Uri.fromFile(file)).await()
                storageRef.downloadUrl.await().toString()
            }

            val now = System.currentTimeMillis()
            val paymentData = mapOf<String, Any>(
                "status" to Reservation.STATUS_PAID,
                "paymentVoucherUrl" to downloadUrl,
                "paymentMethod" to paymentMethod,
                "paidAtMillis" to now
            )

            reservationRef.update(paymentData).await()

            file.delete()

            Result.success(workDataOf(KEY_DOWNLOAD_URL to downloadUrl))
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                failure(e.localizedMessage ?: "No se pudo subir el comprobante")
            }
        }
    }

    private fun failure(message: String): Result =
        Result.failure(workDataOf(KEY_ERROR to message))
}
