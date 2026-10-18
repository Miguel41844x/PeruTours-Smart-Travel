package com.example.perutours.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.work.ExistingWorkPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.perutours.service.UploadPaymentWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * HU07: estado observable de la subida del comprobante.
 * Refleja el ciclo En cola → Subiendo → Enviado → Error.
 */
sealed interface PaymentUploadState {
    data object Idle : PaymentUploadState
    data object Enqueued : PaymentUploadState
    data object Uploading : PaymentUploadState
    data object Uploaded : PaymentUploadState
    data class Error(val message: String) : PaymentUploadState
}

class PaymentRepository(
    private val context: Context,
    private val workManager: WorkManager
) {

    suspend fun saveVoucherLocally(sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val extension = resolveImageExtension(sourceUri)
        val vouchersDir = File(context.filesDir, VOUCHERS_DIRECTORY)
        check(vouchersDir.exists() || vouchersDir.mkdirs()) {
            "No se pudo preparar el almacenamiento local del comprobante"
        }

        val localFile = File(vouchersDir, "voucher_${UUID.randomUUID()}.$extension")
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: throw IllegalArgumentException("No se pudo leer la imagen del comprobante")
            inputStream.use { input ->
                FileOutputStream(localFile).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var totalBytes = 0L
                    while (true) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead < 0) break
                        totalBytes += bytesRead
                        require(PaymentValidator.isValidVoucherSize(totalBytes)) {
                            "El comprobante no debe superar los 10 MB"
                        }
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
        } catch (error: Exception) {
            localFile.delete()
            throw error
        }
        localFile.absolutePath
    }

    /**
     * HU07: encola la subida del comprobante con WorkManager. Devuelve el
     * ID del trabajo REALMENTE activo: con ExistingWorkPolicy.KEEP, si ya
     * hay una subida pendiente para la reserva se conserva la anterior y
     * esta petición se descarta, por lo que la UI debe observar el
     * trabajo que sí se va a ejecutar y no uno desechado.
     */
    suspend fun enqueuePaymentUpload(
        quotationId: String,
        reservationId: String,
        localImagePath: String,
        paymentMethod: String
    ): UUID = withContext(Dispatchers.IO) {
        val uniqueName = "payment_upload_$reservationId"

        val pendingWorkId = workManager
            .getWorkInfosForUniqueWork(uniqueName).get()
            .firstOrNull { !it.state.isFinished }
            ?.id

        if (pendingWorkId != null) {
            // Ya hay una subida en cola: KEEP la conservaría y descartar
            // esta. Se descarta la copia recién creada para no dejar un
            // archivo local huérfano.
            File(localImagePath).delete()
            return@withContext pendingWorkId
        }

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

        workManager.enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.KEEP,
            uploadRequest
        )
        uploadRequest.id
    }

    /**
     * HU07: sigue el WorkInfo del trabajo encolado y lo traduce a un
     * estado de UI para que el cliente vea el progreso real y los
     * errores definitivos de la subida.
     */
    fun observeUpload(workId: UUID): Flow<PaymentUploadState> =
        workManager.getWorkInfoByIdFlow(workId).map { info -> mapWorkInfo(info) }

    private fun mapWorkInfo(info: WorkInfo): PaymentUploadState = when (info.state) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> PaymentUploadState.Enqueued
        WorkInfo.State.RUNNING -> PaymentUploadState.Uploading
        WorkInfo.State.SUCCEEDED -> PaymentUploadState.Uploaded
        WorkInfo.State.FAILED -> PaymentUploadState.Error(
            info.outputData.getString(UploadPaymentWorker.KEY_ERROR)
                ?: "No se pudo subir el comprobante. Intenta nuevamente."
        )
        WorkInfo.State.CANCELLED -> PaymentUploadState.Error(
            "La subida del comprobante fue cancelada."
        )
    }

    private fun resolveImageExtension(uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri)
        val extensionFromMime = mimeType?.let(MimeTypeMap.getSingleton()::getExtensionFromMimeType)
        val extensionFromUri = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        val extension = extensionFromMime
            ?.takeIf(String::isNotBlank)
            ?: extensionFromUri.takeIf(String::isNotBlank)
            ?: "jpg"

        require(PaymentValidator.isValidImageExtension(extension)) {
            "Selecciona una imagen JPG, PNG o WEBP"
        }
        return extension.lowercase()
    }

    private companion object {
        const val VOUCHERS_DIRECTORY = "payment_vouchers"
    }
}
