package com.example.perutours.data.repository

import com.example.perutours.data.model.Payment
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * HU07: pruebas de la revisión del pago por el agente: validación del
 * motivo de rechazo, protección de pagos ya resueltos y aprobación
 * atómica con su reserva.
 */
@RunWith(RobolectricTestRunner::class)
class PaymentReviewRepositoryTest {

    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockUser: FirebaseUser
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockBatch: WriteBatch
    private lateinit var repository: PaymentReviewRepository

    private val agentUid = "agente_1"

    private fun pendingPayment() = Payment(
        id = "res_1",
        reservationId = "res_1",
        clientId = "cliente_1",
        agentId = agentUid,
        method = "Yape",
        amount = 1500.0,
        currency = "PEN",
        voucherUrl = "https://firebasestorage.test/voucher.jpg",
        status = Payment.STATUS_PENDING,
        submittedAtMillis = System.currentTimeMillis()
    )

    @Before
    fun setUp() {
        mockAuth = mockk(relaxed = true)
        mockUser = mockk(relaxed = true)
        mockFirestore = mockk(relaxed = true)
        mockBatch = mockk(relaxed = true)

        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns agentUid
        every { mockFirestore.batch() } returns mockBatch
        every { mockBatch.commit() } returns Tasks.forResult<Void>(null)

        repository = PaymentReviewRepository(
            auth = mockAuth,
            firestore = mockFirestore
        )
    }

    @Test
    fun `getPendingPaymentsForAgent sin sesion devuelve lista vacia`() = runTest {
        every { mockAuth.currentUser } returns null

        val payments = repository.getPendingPaymentsForAgent()

        assertEquals(emptyList<Payment>(), payments)
        verify(exactly = 0) { mockFirestore.collection(any()) }
    }

    @Test
    fun `rejectPayment exige un motivo de al menos 10 caracteres`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlockingShim {
                repository.rejectPayment(pendingPayment(), "Malo")
            }
        }

        assertTrue(exception.message.orEmpty().contains("10"))
        verify(exactly = 0) { mockFirestore.batch() }
    }

    @Test
    fun `approvePayment no permite resolver dos veces el mismo pago`() {
        val alreadyApproved = pendingPayment().copy(status = Payment.STATUS_APPROVED)

        assertThrows(IllegalArgumentException::class.java) {
            runBlockingShim { repository.approvePayment(alreadyApproved) }
        }

        verify(exactly = 0) { mockFirestore.batch() }
    }

    @Test
    fun `approvePayment aprueba el pago y marca la reserva como Pagado`() = runBlockingShim {
        val paymentMockRef = mockk<DocumentReference>(relaxed = true)
        val reservationMockRef = mockk<DocumentReference>(relaxed = true)
        val paymentsCollection = mockk<CollectionReference>(relaxed = true)
        val reservationsCollection = mockk<CollectionReference>(relaxed = true)
        every { mockFirestore.collection("payments") } returns paymentsCollection
        every { mockFirestore.collection("reservations") } returns reservationsCollection
        every { paymentsCollection.document("res_1") } returns paymentMockRef
        every { reservationsCollection.document("res_1") } returns reservationMockRef

        repository.approvePayment(pendingPayment())

        verify { mockBatch.update(paymentMockRef, match<Map<String, Any>> { map ->
            map["status"] == Payment.STATUS_APPROVED && map["reviewedBy"] == agentUid
        }) }
        verify { mockBatch.update(reservationMockRef, match<Map<String, Any>> { map ->
            map["status"] == "Pagado"
        }) }
    }

    @Test
    fun `rejectPayment guarda el motivo y marca la reserva como Pago rechazado`() = runBlockingShim {
        val paymentMockRef = mockk<DocumentReference>(relaxed = true)
        val reservationMockRef = mockk<DocumentReference>(relaxed = true)
        val paymentsCollection = mockk<CollectionReference>(relaxed = true)
        val reservationsCollection = mockk<CollectionReference>(relaxed = true)
        every { mockFirestore.collection("payments") } returns paymentsCollection
        every { mockFirestore.collection("reservations") } returns reservationsCollection
        every { paymentsCollection.document("res_1") } returns paymentMockRef
        every { reservationsCollection.document("res_1") } returns reservationMockRef

        repository.rejectPayment(pendingPayment(), "El comprobante está ilegible, sube otra foto.")

        verify { mockBatch.update(paymentMockRef, match<Map<String, Any>> { map ->
            map["status"] == Payment.STATUS_REJECTED &&
                map["rejectionReason"] == "El comprobante está ilegible, sube otra foto."
        }) }
        verify { mockBatch.update(reservationMockRef, match<Map<String, Any>> { map ->
            map["status"] == "Pago rechazado"
        }) }
    }

    /** Ejecuta una corrutina suspendida dentro de assertThrows. */
    private fun <T> runBlockingShim(block: suspend () -> T): T = runBlocking { block() }
}
