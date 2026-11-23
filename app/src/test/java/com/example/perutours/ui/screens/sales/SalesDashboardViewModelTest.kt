package com.example.perutours.ui.screens.sales

import com.example.perutours.data.model.SalesAnalytics
import com.example.perutours.data.repository.SalesDashboardDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
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
class SalesDashboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsSalesAnalyticsOnCreation() = runTest(dispatcher) {
        val expected = SalesAnalytics(totalQuotations = 8, approvedSales = 3)
        val viewModel = SalesDashboardViewModel(FakeSalesDashboardRepository(expected))

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(expected, viewModel.uiState.value.analytics)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun exposesFriendlyMessageWhenRepositoryFails() = runTest(dispatcher) {
        val viewModel = SalesDashboardViewModel(
            FakeSalesDashboardRepository(error = IllegalStateException("sin permisos"))
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(
            "No pudimos actualizar las estadísticas. Revisa tu conexión.",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun repeatedRefreshBeforeDispatchStartsOnlyOneRequest() = runTest(dispatcher) {
        var calls = 0
        val viewModel = SalesDashboardViewModel(object : SalesDashboardDataSource {
            override suspend fun fetchSalesAnalytics(): SalesAnalytics {
                calls++
                return SalesAnalytics(totalQuotations = 2)
            }
        })

        viewModel.refresh()
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(1, calls)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun cancellationDoesNotBecomeANetworkErrorOrBlockTheNextRefresh() = runTest(dispatcher) {
        var calls = 0
        val viewModel = SalesDashboardViewModel(object : SalesDashboardDataSource {
            override suspend fun fetchSalesAnalytics(): SalesAnalytics {
                if (calls++ == 0) throw CancellationException("consulta cancelada")
                return SalesAnalytics(totalQuotations = 3)
            }
        })
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.analytics.totalQuotations)
    }

    @Test
    fun keepsExistingMetricsVisibleWhileRefreshWaitsOrFails() = runTest(dispatcher) {
        val nextResponse = CompletableDeferred<SalesAnalytics>()
        var calls = 0
        val expected = SalesAnalytics(totalQuotations = 8, approvedSales = 3)
        val viewModel = SalesDashboardViewModel(object : SalesDashboardDataSource {
            override suspend fun fetchSalesAnalytics(): SalesAnalytics =
                if (calls++ == 0) expected else nextResponse.await()
        })
        advanceUntilIdle()
        viewModel.refresh()
        assertEquals(expected, viewModel.uiState.value.analytics)
        nextResponse.completeExceptionally(IllegalStateException("sin conexión"))
        advanceUntilIdle()

        assertEquals(expected, viewModel.uiState.value.analytics)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private class FakeSalesDashboardRepository(
        private val analytics: SalesAnalytics = SalesAnalytics(),
        private val error: Exception? = null
    ) : SalesDashboardDataSource {
        override suspend fun fetchSalesAnalytics(): SalesAnalytics {
            error?.let { throw it }
            return analytics
        }
    }
}
