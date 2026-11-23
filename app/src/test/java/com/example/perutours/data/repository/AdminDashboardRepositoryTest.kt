package com.example.perutours.data.repository

import com.example.perutours.data.local.AdminCacheDao
import com.example.perutours.data.local.AdminMetricsEntity
import com.example.perutours.data.local.ReservationCacheDao
import com.example.perutours.data.local.ReservationEntity
import com.example.perutours.data.model.AdminMetrics
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdminDashboardRepositoryTest {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var reservations: CollectionReference
    private lateinit var providers: CollectionReference
    private lateinit var metricsCache: AdminCacheDao
    private lateinit var reservationCache: ReservationCacheDao
    private lateinit var repository: AdminDashboardRepository
    private lateinit var pageQuery: Query

    @Before
    fun setUp() {
        firestore = mockk()
        reservations = mockk()
        metricsCache = mockk()
        reservationCache = mockk()
        pageQuery = mockk()
        repository = AdminDashboardRepository(firestore, metricsCache, reservationCache)
        val emptySnapshot = mockk<QuerySnapshot> { every { documents } returns emptyList() }
        listOf("providers", "catalog_services", "destinations", "packages", "users").forEach { name ->
            val collection = mockk<CollectionReference>()
            every { firestore.collection(name) } returns collection
            every { collection.get(Source.SERVER) } returns Tasks.forResult(emptySnapshot)
        }
        providers = firestore.collection("providers")
        coEvery { metricsCache.cacheMetrics(any()) } returns Unit
        every { firestore.collection("reservations") } returns reservations
        every { reservations.orderBy("createdAtMillis", Query.Direction.DESCENDING) } returns pageQuery
        every { pageQuery.limit(15) } returns pageQuery
        coEvery { metricsCache.getCachedMetrics() } returns AdminMetricsEntity.fromMetrics(AdminMetrics(totalProviders = 2, updatedAtMillis = 1L))
        coEvery { reservationCache.getCachedReservations("Todos", 15) } returns
            listOf(ReservationEntity(id = "cached"))
    }

    @Test
    fun connectionFailureReturnsTheRoomMetricsAsCached() = runTest {
        every { providers.get(Source.SERVER) } returns Tasks.forException(
            firestoreError(FirebaseFirestoreException.Code.UNAVAILABLE)
        )

        val metrics = repository.fetchAdminMetrics()

        assertTrue(metrics.isCached)
        assertEquals(2, metrics.totalProviders)
    }

    @Test
    fun permissionsAndMissingIndexesAreNotMaskedByOldMetrics() = runTest {
        listOf(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            FirebaseFirestoreException.Code.FAILED_PRECONDITION
        ).forEach { code ->
            val error = firestoreError(code)
            every { providers.get(Source.SERVER) } returns Tasks.forException(error)

            assertSame(error, runCatching { repository.fetchAdminMetrics() }.exceptionOrNull())
        }
        coVerify(exactly = 0) { metricsCache.getCachedMetrics() }
    }

    @Test
    fun cancelledMetricsDoNotReadTheOfflineCache() = runTest {
        val cancelled = CancellationException("consulta cancelada")
        every { providers.get(Source.SERVER) } returns Tasks.forException(cancelled)

        val error = runCatching { repository.fetchAdminMetrics() }.exceptionOrNull()
        assertTrue(error is CancellationException)
        assertEquals(cancelled.message, error?.message)
        coVerify(exactly = 0) { metricsCache.getCachedMetrics() }
    }

    @Test
    fun disconnectedFirstPageCanUseCachedReservations() = runTest {
        every { pageQuery.get(Source.SERVER) } returns Tasks.forException<QuerySnapshot>(
            firestoreError(FirebaseFirestoreException.Code.UNAVAILABLE)
        )

        val (rows, cursor) = repository.fetchReservations(statusFilter = "Todos")

        assertEquals(listOf("cached"), rows.map { it.id })
        assertEquals(null, cursor)
    }

    @Test
    fun disconnectedNextPageNeverAppendsTheFirstCachedPageAgain() = runTest {
        val cursor = mockk<DocumentSnapshot>()
        val error = firestoreError(FirebaseFirestoreException.Code.UNAVAILABLE)
        every { pageQuery.startAfter(cursor) } returns pageQuery
        every { pageQuery.get(Source.SERVER) } returns Tasks.forException<QuerySnapshot>(error)

        assertSame(error, runCatching {
            repository.fetchReservations(statusFilter = "Todos", lastDocument = cursor)
        }.exceptionOrNull())
        coVerify(exactly = 0) { reservationCache.getCachedReservations(any(), any()) }
    }

    @Test
    fun permissionFailureNeverReturnsCachedReservations() = runTest {
        val error = firestoreError(FirebaseFirestoreException.Code.PERMISSION_DENIED)
        every { pageQuery.get(Source.SERVER) } returns Tasks.forException<QuerySnapshot>(error)

        assertSame(error, runCatching {
            repository.fetchReservations(statusFilter = "Todos")
        }.exceptionOrNull())
        coVerify(exactly = 0) { reservationCache.getCachedReservations(any(), any()) }
    }

    private fun firestoreError(code: FirebaseFirestoreException.Code) =
        FirebaseFirestoreException("Error simulado para la prueba", code)
}
