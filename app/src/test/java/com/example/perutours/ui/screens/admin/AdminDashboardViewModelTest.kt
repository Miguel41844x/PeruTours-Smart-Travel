package com.example.perutours.ui.screens.admin

import com.example.perutours.data.model.AdminMetrics
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.repository.AdminDashboardRepository
import com.google.firebase.firestore.DocumentSnapshot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminDashboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: AdminDashboardRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        coEvery { repository.fetchAdminMetrics() } returns AdminMetrics(totalProviders = 30, updatedAtMillis = 1L)
        coEvery { repository.fetchReservations(any(), any(), any()) } returns (emptyList<Reservation>() to null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testConstants() {
        assertEquals(15, AdminDashboardConstants.DEFAULT_PAGE_SIZE)
        assertEquals(5, AdminDashboardConstants.AVAILABLE_FILTERS.size)
        assertEquals("Todos", AdminDashboardConstants.AVAILABLE_FILTERS[0])
        assertEquals("Pagado", AdminDashboardConstants.AVAILABLE_FILTERS[3])
    }

    @Test
    fun testUiStateDefaultValues() {
        val state = AdminDashboardUiState()
        assertEquals(false, state.isLoading)
        assertEquals("Todos", state.selectedFilterStatus)
        assertEquals(0, state.reservations.size)
    }

    @Test
    fun testAdminMetricsDefaults() {
        val metrics = AdminMetrics()
        assertEquals(0, metrics.totalProviders)
        assertEquals(0, metrics.totalPackages)
        assertEquals(0, metrics.totalUsers)
        assertEquals(true, metrics.packagesByStatus.isEmpty())
    }

    @Test
    fun oldFilterCannotReplaceTheLatestSelection() = runTest(dispatcher) {
        val viewModel = AdminDashboardViewModel(repository)
        advanceUntilIdle()
        val oldResponse = CompletableDeferred<Pair<List<Reservation>, DocumentSnapshot?>>()
        coEvery { repository.fetchReservations(any(), "Confirmada", null) } coAnswers {
            withContext(NonCancellable) { oldResponse.await() }
        }
        coEvery { repository.fetchReservations(any(), "Pagado", null) } returns
            (listOf(Reservation(id = "paid", status = "Pagado")) to null)

        viewModel.setFilterStatus("Confirmada")
        runCurrent()
        viewModel.setFilterStatus("Pagado")
        runCurrent()
        oldResponse.complete(listOf(Reservation(id = "old", status = "Confirmada")) to null)
        advanceUntilIdle()

        assertEquals("Pagado", viewModel.uiState.value.selectedFilterStatus)
        assertEquals(listOf("paid"), viewModel.uiState.value.reservations.map { it.id })
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun changingFilterRemovesPreviousRowsWhileTheNewPageLoads() = runTest(dispatcher) {
        coEvery { repository.fetchReservations(any(), "Todos", null) } returns
            (listOf(Reservation(id = "previous")) to null)
        val viewModel = AdminDashboardViewModel(repository)
        advanceUntilIdle()
        val nextResponse = CompletableDeferred<Pair<List<Reservation>, DocumentSnapshot?>>()
        coEvery { repository.fetchReservations(any(), "Pagado", null) } coAnswers { nextResponse.await() }

        viewModel.setFilterStatus("Pagado")
        assertTrue(viewModel.uiState.value.isLoadingReservations)
        assertTrue(viewModel.uiState.value.reservations.isEmpty())
        runCurrent()
        nextResponse.complete(emptyList<Reservation>() to null)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoadingReservations)
    }

    @Test
    fun aPendingNextPageCannotAppendRowsAfterChangingFilter() = runTest(dispatcher) {
        val cursor = mockk<DocumentSnapshot>()
        val firstPage = List(15) { Reservation(id = "first_$it") }
        coEvery { repository.fetchReservations(any(), "Todos", null) } returns (firstPage to cursor)
        val viewModel = AdminDashboardViewModel(repository)
        advanceUntilIdle()
        val nextResponse = CompletableDeferred<Pair<List<Reservation>, DocumentSnapshot?>>()
        coEvery { repository.fetchReservations(any(), "Todos", cursor) } coAnswers {
            withContext(NonCancellable) { nextResponse.await() }
        }
        coEvery { repository.fetchReservations(any(), "Pagado", null) } returns
            (listOf(Reservation(id = "paid", status = "Pagado")) to null)

        viewModel.loadMoreReservations()
        runCurrent()
        viewModel.setFilterStatus("Pagado")
        runCurrent()
        nextResponse.complete(listOf(Reservation(id = "old_next_page")) to null)
        advanceUntilIdle()

        assertEquals(listOf("paid"), viewModel.uiState.value.reservations.map { it.id })
        assertFalse(viewModel.uiState.value.isLoadingMore)
    }

    @Test
    fun repeatedLoadMoreStartsOnlyOneRequestAndDoesNotDuplicateRows() = runTest(dispatcher) {
        val cursor = mockk<DocumentSnapshot>()
        val firstPage = List(15) { Reservation(id = "first_$it") }
        coEvery { repository.fetchReservations(any(), "Todos", null) } returns (firstPage to cursor)
        coEvery { repository.fetchReservations(any(), "Todos", cursor) } returns
            (listOf(firstPage.last(), Reservation(id = "next")) to null)
        val viewModel = AdminDashboardViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMoreReservations()
        viewModel.loadMoreReservations()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.fetchReservations(any(), "Todos", cursor) }
        assertEquals(16, viewModel.uiState.value.reservations.size)
        assertTrue(viewModel.uiState.value.endReached)
    }

    @Test
    fun filteringDoesNotCancelMetricsAndKeepsTheirErrorVisible() = runTest(dispatcher) {
        val metricsResponse = CompletableDeferred<AdminMetrics>()
        coEvery { repository.fetchAdminMetrics() } coAnswers { metricsResponse.await() }
        val viewModel = AdminDashboardViewModel(repository)
        runCurrent()
        viewModel.setFilterStatus("Pagado")
        runCurrent()
        metricsResponse.completeExceptionally(IllegalStateException("sin permisos"))
        advanceUntilIdle()

        assertEquals("Pagado", viewModel.uiState.value.selectedFilterStatus)
        assertNotNull(viewModel.uiState.value.metricsErrorMessage)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun refreshCancelsAnOlderNextPageAndKeepsTheRefreshedList() = runTest(dispatcher) {
        val cursor = mockk<DocumentSnapshot>()
        coEvery { repository.fetchReservations(any(), "Todos", null) } returns
            (List(15) { Reservation(id = "initial_$it") } to cursor)
        val viewModel = AdminDashboardViewModel(repository)
        advanceUntilIdle()
        val nextResponse = CompletableDeferred<Pair<List<Reservation>, DocumentSnapshot?>>()
        coEvery { repository.fetchReservations(any(), "Todos", cursor) } coAnswers {
            withContext(NonCancellable) { nextResponse.await() }
        }
        viewModel.loadMoreReservations()
        runCurrent()
        coEvery { repository.fetchReservations(any(), "Todos", null) } returns
            (listOf(Reservation(id = "fresh")) to null)

        viewModel.refresh()
        runCurrent()
        nextResponse.complete(listOf(Reservation(id = "stale")) to null)
        advanceUntilIdle()

        assertEquals(listOf("fresh"), viewModel.uiState.value.reservations.map { it.id })
        assertFalse(viewModel.uiState.value.isLoadingMore)
    }
}
