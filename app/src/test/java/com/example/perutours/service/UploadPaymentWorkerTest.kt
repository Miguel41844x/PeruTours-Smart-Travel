package com.example.perutours.service

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.perutours.data.model.Payment
import com.example.perutours.data.model.Reservation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * HU07: pruebas del Worker de pago. El comprobante pasa la reserva a
 * 'Pago enviado' con el pago 'Pendiente' (nunca a 'Pagado'), toma el
 * importe de la reserva en Firestore y es idempotente.
 */
@RunWith(RobolectricTestRunner::class)
class UploadPaymentWorkerTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockUser: FirebaseUser
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockStorage: FirebaseStorage
    private lateinit var mockStorageRef: StorageReference
    private lateinit var mockFileRef: StorageReference
    private lateinit var mockReservations: CollectionReference
    private lateinit var mockPayments: CollectionReference
    private lateinit var mockReservationRef: DocumentReference
    private lateinit var mockPaymentRef: DocumentReference
    private lateinit var mockReservationSnapshot: DocumentSnapshot
    private lateinit var mockBatch: WriteBatch

    private val testUid = "cliente_1"
    private val reservationId = "res_1"
    private val fileName = "voucher_test.jpg"

    @Before
    fun setUp() {
        mockAuth = mockk(relaxed = true)
        mockUser = mockk(relaxed = true)
        mockFirestore = mockk(relaxed = true)
        mockStorage = mockk(relaxed = true)
        mockStorageRef = mockk(relaxed = true)
        mockFileRef = mockk(relaxed = true)
        mockReservations = mockk(relaxed = true)
        mockPayments = mockk(relaxed = true)
        mockReservationRef = mockk(relaxed = true)
        mockPaymentRef = mockk(relaxed = true)
        mockReservationSnapshot = mockk(relaxed = true)
        mockBatch = mockk(relaxed = true)

        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns testUid

        every { mockFirestore.collection("reservations") } returns mockReservations
        every { mockFirestore.collection("payments") } returns mockPayments
        every { mockReservations.document(reservationId) } returns mockReservationRef
        every { mockPayments.document(reservationId) } returns mockPaymentRef
        every { mockFirestore.batch() } returns mockBatch
        every { mockBatch.commit() } returns Tasks.forResult<Void>(null)

        every { mockStorage.reference } returns mockStorageRef
        every {
            mockStorageRef.child("payment_vouchers/$testUid/$reservationId/$fileName")
        } returns mockFileRef

        startKoin {
            modules(
                module {
                    single<FirebaseAuth> { mockAuth }
                    single<FirebaseFirestore> { mockFirestore }
                    single<FirebaseStorage> { mockStorage }
                }
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun stubReservation(
        status: String,
        clientId: String = testUid,
        totalAmount: Double = 1500.0
    ) {
        every { mockReservationRef.get() } returns Tasks.forResult(mockReservationSnapshot)
        every { mockReservationSnapshot.exists() } returns true
        every { mockReservationSnapshot.getString("clientId") } returns clientId
        every { mockReservationSnapshot.getString("quotationId") } returns reservationId
        every { mockReservationSnapshot.getString("status") } returns status
        every { mockReservationSnapshot.getString("agentId") } returns "agente_1"
        every { mockReservationSnapshot.getString("currency") } returns "PEN"
        every { mockReservationSnapshot.getDouble("totalAmount") } returns totalAmount
    }

    private fun stubExistingVoucherInStorage() {
        every { mockFileRef.metadata } returns Tasks.forResult(mockk<StorageMetadata>())
        every { mockFileRef.downloadUrl } returns
            Tasks.forResult(Uri.parse("https://firebasestorage.test/voucher.jpg"))
    }

    private fun createLocalVoucher(): File =
        File(context.cacheDir, fileName).apply { writeText("comprobante") }

    private fun buildWorker(method: String = "Yape"): UploadPaymentWorker {
        val voucherFile = createLocalVoucher()
        val input = Data.Builder()
            .putString(UploadPaymentWorker.KEY_QUOTATION_ID, reservationId)
            .putString(UploadPaymentWorker.KEY_RESERVATION_ID, reservationId)
            .putString(UploadPaymentWorker.KEY_IMAGE_PATH, voucherFile.absolutePath)
            .putString(UploadPaymentWorker.KEY_PAYMENT_METHOD, method)
            .build()
        return TestListenableWorkerBuilder<UploadPaymentWorker>(context)
            .setInputData(input)
            .build()
    }

    @Test
    fun `al subir el comprobante la reserva pasa a Pago enviado y el pago queda Pendiente`() =
        runBlocking {
            stubReservation(Reservation.STATUS_CONFIRMED)
            stubExistingVoucherInStorage()
            val worker = buildWorker(method = "Yape")

            val result = worker.doWork()

            assertTrue(result is ListenableWorker.Result.Success)
            assertEquals(
                "https://firebasestorage.test/voucher.jpg",
                result.outputData.getString(UploadPaymentWorker.KEY_DOWNLOAD_URL)
            )

            // El pago se crea con el importe leído de la reserva.
            val paymentSlot = slot<Payment>()
            verify { mockBatch.set(mockPaymentRef, capture(paymentSlot)) }
            val payment = paymentSlot.captured
            assertEquals(reservationId, payment.id)
            assertEquals(reservationId, payment.reservationId)
            assertEquals(testUid, payment.clientId)
            assertEquals("agente_1", payment.agentId)
            assertEquals("Yape", payment.method)
            assertEquals(1500.0, payment.amount, 0.01)
            assertEquals("PEN", payment.currency)
            assertEquals(Payment.STATUS_PENDING, payment.status)

            // La reserva solo cambia estado y vínculo; nunca a Pagado.
            val updateSlot = slot<Map<String, Any>>()
            verify { mockBatch.update(mockReservationRef, capture(updateSlot)) }
            assertEquals(
                Reservation.STATUS_PAYMENT_PENDING,
                updateSlot.captured["status"]
            )
            assertEquals(reservationId, updateSlot.captured["paymentId"])
        }

    @Test
    fun `es idempotente cuando la reserva ya esta Pagado`() = runBlocking {
        stubReservation(Reservation.STATUS_PAID)
        val voucherFile = createLocalVoucher()
        val worker = buildWorker()

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        verify(exactly = 0) { mockFirestore.batch() }
        assertFalse(voucherFile.exists())
    }

    @Test
    fun `es idempotente cuando el comprobante ya esta en revision`() = runBlocking {
        stubReservation(Reservation.STATUS_PAYMENT_PENDING)
        val voucherFile = createLocalVoucher()
        val worker = buildWorker()

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        verify(exactly = 0) { mockFirestore.batch() }
        assertFalse(voucherFile.exists())
    }

    @Test
    fun `rechaza un metodo de pago fuera del catalogo`() = runBlocking {
        stubReservation(Reservation.STATUS_CONFIRMED)
        val worker = buildWorker(method = "Efectivo")

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        val message = result.outputData.getString(UploadPaymentWorker.KEY_ERROR).orEmpty()
        assertTrue(message.contains("método de pago"))
        verify(exactly = 0) { mockFirestore.batch() }
    }

    @Test
    fun `rechaza subir el comprobante de una reserva ajena`() = runBlocking {
        stubReservation(Reservation.STATUS_CONFIRMED, clientId = "otro_cliente")
        val worker = buildWorker()

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        val message = result.outputData.getString(UploadPaymentWorker.KEY_ERROR).orEmpty()
        assertTrue(message.contains("no pertenece"))
        verify(exactly = 0) { mockFirestore.batch() }
    }
}
