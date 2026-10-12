package com.example.perutours.data.repository

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.perutours.service.UploadPaymentWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class PaymentRepository(
    private val context: Context,
    private val workManager: WorkManager
) {

    /**
     * Guarda la foto capturada o seleccionada en la caché local para que sobreviva
     * aunque la app se cierre o el dispositivo esté en Modo Avión.
     */
    suspend fun saveVoucherLocally(sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(sourceUri)
            ?: throw IllegalArgumentException("No se pudo leer la imagen del comprobante")
        val localFile = File(context.cacheDir, "voucher_${UUID.randomUUID()}.jpg")
        val outputStream = FileOutputStream(localFile)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        localFile.absolutePath
    }

    /**
     * Criterio 2: Encolar la subida con WorkManager.
     * Restricción: NetworkType.CONNECTED -> Solo se ejecuta cuando haya conexión a internet.
     */
    fun enqueuePaymentUpload(
        quotationId: String,
        reservationId: String,
        localImagePath: String,
        paymentMethod: String
    ): UUID {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadRequest = OneTimeWorkRequestBuilder<UploadPaymentWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    UploadPaymentWorker.KEY_QUOTATION_ID to quotationId,
                    UploadPaymentWorker.KEY_RESERVATION_ID to reservationId,
                    UploadPaymentWorker.KEY_IMAGE_PATH to localImagePath,
                    UploadPaymentWorker.KEY_PAYMENT_METHOD to paymentMethod
                )
            )
            .build()

        workManager.enqueue(uploadRequest)
        return uploadRequest.id
    }
}
