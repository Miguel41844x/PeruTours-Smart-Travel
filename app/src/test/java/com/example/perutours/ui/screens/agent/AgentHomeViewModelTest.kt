package com.example.perutours.ui.screens.agent

import com.example.perutours.data.model.QuotationStatusMetric
import com.example.perutours.data.model.TravelRequestStatus
import com.example.perutours.data.repository.AgentDashboard
import com.example.perutours.data.repository.AgentDashboardRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AgentHomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<AgentDashboardRepository>()
    private val dashboard = AgentDashboard(
        name = "Agente",
        photoUrl = "",
        statuses = listOf(
            QuotationStatusMetric(TravelRequestStatus.SUBMITTED.label, 4),
            QuotationStatusMetric(TravelRequestStatus.QUOTED.label, 2)
        ),
        pendingPayments = 3
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { repository.fetch() } returns dashboard
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsPortfolioAndPendingPaymentsWithoutDuplicatingRefreshes() = runTest(dispatcher) {
        val viewModel = AgentHomeViewModel(repository)
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.pendingCount)
        assertEquals(2, viewModel.uiState.value.quotedCount)
        assertEquals(3, viewModel.uiState.value.pendingPayments)
        coVerify(exactly = 1) { repository.fetch() }
    }

    @Test
    fun cancellationLeavesTheDashboardReadyToRetry() = runTest(dispatcher) {
        coEvery { repository.fetch() } throws CancellationException("consulta cancelada")
        val viewModel = AgentHomeViewModel(repository)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertNull(viewModel.uiState.value.errorMessage)

        coEvery { repository.fetch() } returns dashboard
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.pendingCount)
        assertFalse(viewModel.uiState.value.isLoadingCounts)
    }

    @Test
    fun failedRefreshPreservesTheLoadedPortfolio() = runTest(dispatcher) {
        val viewModel = AgentHomeViewModel(repository)
        advanceUntilIdle()
        coEvery { repository.fetch() } throws IllegalStateException("sin conexión")

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.pendingCount)
        assertEquals(3, viewModel.uiState.value.pendingPayments)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals("No pudimos actualizar tu cartera. Inténtalo nuevamente.", viewModel.uiState.value.errorMessage)
    }
}
