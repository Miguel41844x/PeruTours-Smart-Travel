package com.example.perutours.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.work.ExistingWorkPolicy
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
                        require(totalBytes <= MAX_VOUCHER_BYTES) {
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

        workManager.enqueueUniqueWork(
            "payment_upload_$reservationId",
            ExistingWorkPolicy.KEEP,
            uploadRequest
        )
        return uploadRequest.id
    }

    private fun resolveImageExtension(uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri)
        val extensionFromMime = mimeType?.let(MimeTypeMap.getSingleton()::getExtensionFromMimeType)
        val extensionFromUri = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        val extension = extensionFromMime
            ?.takeIf(String::isNotBlank)
            ?: extensionFromUri.takeIf(String::isNotBlank)
            ?: "jpg"

        require(extension.lowercase() in ALLOWED_IMAGE_EXTENSIONS) {
            "Selecciona una imagen JPG, PNG o WEBP"
        }
        return extension.lowercase()
    }

    private companion object {
        const val VOUCHERS_DIRECTORY = "payment_vouchers"
        const val MAX_VOUCHER_BYTES = 10L * 1024L * 1024L
        val ALLOWED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
    }
}
