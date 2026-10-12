package com.example.perutours.service

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

class UploadPaymentWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_QUOTATION_ID = "quotation_id"
        const val KEY_RESERVATION_ID = "reservation_id"
        const val KEY_IMAGE_PATH = "image_path"
        const val KEY_PAYMENT_METHOD = "payment_method"
        const val KEY_DOWNLOAD_URL = "download_url"
    }

    override suspend fun doWork(): Result {
        val quotationId = inputData.getString(KEY_QUOTATION_ID).orEmpty()
        val reservationId = inputData.getString(KEY_RESERVATION_ID).orEmpty()
        val imagePath = inputData.getString(KEY_IMAGE_PATH).orEmpty()
        val paymentMethod = inputData.getString(KEY_PAYMENT_METHOD) ?: "Transferencia / Yape"

        if (imagePath.isBlank()) {
            return Result.failure(workDataOf("error" to "Ruta de comprobante inválida"))
        }

        val file = File(imagePath)
        if (!file.exists()) {
            return Result.failure(workDataOf("error" to "El comprobante no existe en disco local"))
        }

        return try {
            val storage = FirebaseStorage.getInstance()
            val firestore = FirebaseFirestore.getInstance()

            val fileName = "voucher_${System.currentTimeMillis()}_${file.name}"
            val storageRef = storage.reference.child("payment_vouchers/$fileName")

            // 1. Subir a Firebase Storage
            val fileUri = Uri.fromFile(file)
            storageRef.putFile(fileUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val now = System.currentTimeMillis()

            // 2. Criterio 3: Cambiar estado a 'Pagado' en Firestore
            val paymentData = hashMapOf(
                "status" to "Pagado",
                "paymentVoucherUrl" to downloadUrl,
                "paymentMethod" to paymentMethod,
                "paidAtMillis" to now
            )

            // Actualizar documento de cotización
            if (quotationId.isNotBlank()) {
                firestore.collection("cotizaciones")
                    .document(quotationId)
                    .set(paymentData, SetOptions.merge())
                    .await()
            }

            // Actualizar documento de reserva
            if (reservationId.isNotBlank()) {
                firestore.collection("reservations")
                    .document(reservationId)
                    .set(paymentData, SetOptions.merge())
                    .await()
            }

            // Limpieza del archivo temporal local una vez subido
            runCatching { file.delete() }

            Result.success(workDataOf(KEY_DOWNLOAD_URL to downloadUrl))
        } catch (e: Exception) {
            // Si falló por falta de red o error transitorio, reintenta automáticamente
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(workDataOf("error" to (e.localizedMessage ?: "Error desconocido")))
            }
        }
    }
}