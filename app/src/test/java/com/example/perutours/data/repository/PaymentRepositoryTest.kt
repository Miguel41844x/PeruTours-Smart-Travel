package com.example.perutours.data.repository

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.perutours.service.UploadPaymentWorker
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID

/**
 * HU07: pruebas del repositorio de pagos: copia local del comprobante,
 * encolado con el método elegido y mapeo del WorkInfo a estados de UI.
 */
@RunWith(RobolectricTestRunner::class)
class PaymentRepositoryTest {

    private lateinit var workManager: WorkManager
    private lateinit var repository: PaymentRepository

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        workManager = mockk(relaxed = true)
        repository = PaymentRepository(
            context = context,
            workManager = workManager
        )
    }

    @Test
    fun `saveVoucherLocally copia el comprobante al almacenamiento interno`() = runTest {
        val content = byteArrayOf(1, 2, 3, 4, 5)
        val sourceFile = File(context.cacheDir, "source_voucher.jpg").apply {
            writeBytes(content)
        }
        val sourceUri = Uri.fromFile(sourceFile)
        shadowOf(context.contentResolver)
            .registerInputStream(sourceUri, ByteArrayInputStream(content))

        val localPath = repository.saveVoucherLocally(sourceUri)

        val localFile = File(localPath)
        assertTrue(localFile.exists())
        assertTrue(localPath.contains("payment_vouchers"))
        assertTrue(localPath.endsWith(".jpg"))
        assertTrue(content.contentEquals(localFile.readBytes()))
    }

    @Test
    fun `saveVoucherLocally rechaza imagenes que no sean jpg png o webp`() = runTest {
        val sourceFile = File(context.cacheDir, "source_voucher.gif").apply {
            writeBytes(byteArrayOf(1))
        }
        val result = runCatching {
            repository.saveVoucherLocally(Uri.fromFile(sourceFile))
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `enqueuePaymentUpload devuelve el id del trabajo y envia el metodo elegido`() {
        val requestSlot = slot<OneTimeWorkRequest>()
        every {
            workManager.enqueueUniqueWork(any(), any(), capture(requestSlot))
        } returns mockk<Operation>()

        val workId = repository.enqueuePaymentUpload(
            quotationId = "res_1",
            reservationId = "res_1",
            localImagePath = "/tmp/voucher.jpg",
            paymentMethod = "Yape"
        )

        assertEquals(workId, requestSlot.captured.id)
        verify {
            workManager.enqueueUniqueWork(
                "payment_upload_res_1",
                ExistingWorkPolicy.KEEP,
                any<OneTimeWorkRequest>()
            )
        }

        val inputData = requestSlot.captured.workSpec.input
        assertEquals("res_1", inputData.getString(UploadPaymentWorker.KEY_QUOTATION_ID))
        assertEquals("res_1", inputData.getString(UploadPaymentWorker.KEY_RESERVATION_ID))
        assertEquals("/tmp/voucher.jpg", inputData.getString(UploadPaymentWorker.KEY_IMAGE_PATH))
        assertEquals("Yape", inputData.getString(UploadPaymentWorker.KEY_PAYMENT_METHOD))
    }

    @Test
    fun `observeUpload traduce el WorkInfo a los estados de subida`() = runTest {
        val workId = UUID.randomUUID()
        val workInfo = mockk<WorkInfo>()
        every { workInfo.state } returns WorkInfo.State.RUNNING
        every { workManager.getWorkInfoByIdFlow(workId) } returns flowOf(workInfo)

        val states = repository.observeUpload(workId).toList()

        assertEquals(listOf(PaymentUploadState.Uploading), states)

        every { workInfo.state } returns WorkInfo.State.FAILED
        every { workInfo.outputData } returns androidx.work.Data.Builder()
            .putString(UploadPaymentWorker.KEY_ERROR, "Falló la subida")
            .build()

        val failedStates = repository.observeUpload(workId).toList()

        assertEquals(
            PaymentUploadState.Error("Falló la subida"),
            failedStates.single()
        )
        assertFalse(failedStates.single() is PaymentUploadState.Uploaded)
    }
}
