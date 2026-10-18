package com.example.perutours.service

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.perutours.data.model.Payment
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.repository.PaymentValidator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.tasks.await
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * HU07: Sube el comprobante a Storage y registra el pago en la reserva.
 *
 * Flujo: la reserva pasa a 'Pago enviado' y el pago queda 'Pendiente'
 * para que el agente lo verifique. La reserva NO se marca 'Pagado' aquí:
 * eso solo lo hace el agente al aprobar el comprobante.
 *
 * El ID del pago es el ID de la reserva, por lo que un segundo envío
 * sobrescribe el intento anterior en lugar de crear pagos duplicados.
 */
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
        const val COLLECTION_PAYMENTS = "payments"
    }

    override suspend fun doWork(): Result {
        val quotationId = inputData.getString(KEY_QUOTATION_ID).orEmpty()
        val reservationId = inputData.getString(KEY_RESERVATION_ID).orEmpty()
        val imagePath = inputData.getString(KEY_IMAGE_PATH).orEmpty()
        val paymentMethod = inputData.getString(KEY_PAYMENT_METHOD).orEmpty()

        if (quotationId.isBlank() || reservationId.isBlank() || imagePath.isBlank()) {
            return failure("Los datos del pago están incompletos")
        }
        if (!PaymentValidator.isValidMethod(paymentMethod)) {
            return failure("Selecciona un método de pago válido (Yape, Plin o Transferencia BCP)")
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

            when (reservation.getString("status")) {
                // Idempotencia: si el pago ya fue resuelto o está en revisión,
                // no se vuelve a enviar ni se crean pagos duplicados.
                Reservation.STATUS_PAID, Reservation.STATUS_PAYMENT_PENDING -> {
                    file.delete()
                    return Result.success(workDataOf(KEY_DOWNLOAD_URL to ""))
                }
                // Solo una reserva Confirmada o con pago rechazado permite
                // registrar (o reenviar) el comprobante.
                Reservation.STATUS_CONFIRMED, Reservation.STATUS_PAYMENT_REJECTED -> Unit
                else -> return failure("La reserva no está disponible para registrar el pago")
            }

            // El importe y la moneda SIEMPRE salen de la reserva en
            // Firestore; jamás del valor recibido por navegación.
            val totalAmount = reservation.getDouble("totalAmount")
                ?: return failure("La reserva no tiene un importe válido")
            val currency = reservation.getString("currency")
                ?: return failure("La reserva no tiene una moneda válida")

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

            val payment = Payment(
                id = reservationId,
                reservationId = reservationId,
                quotationId = quotationId,
                clientId = userId,
                agentId = reservation.getString("agentId").orEmpty(),
                method = paymentMethod,
                amount = totalAmount,
                currency = currency,
                voucherUrl = downloadUrl,
                status = Payment.STATUS_PENDING,
                submittedAtMillis = System.currentTimeMillis()
            )

            val batch = firestore.batch()
            batch.set(
                firestore.collection(COLLECTION_PAYMENTS).document(reservationId),
                payment
            )
            batch.update(
                reservationRef,
                mapOf(
                    "status" to Reservation.STATUS_PAYMENT_PENDING,
                    "paymentId" to reservationId
                )
            )
            batch.commit().await()

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
