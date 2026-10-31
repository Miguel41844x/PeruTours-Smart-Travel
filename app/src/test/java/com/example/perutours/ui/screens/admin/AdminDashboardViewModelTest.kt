package com.example.perutours.ui.screens.admin

import com.example.perutours.data.model.AdminMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

class AdminDashboardViewModelTest {

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
        assertEquals(0.0, metrics.totalRevenue, 0.001)
        assertEquals(0, metrics.totalReservations)
        assertEquals(0, metrics.activeCustomers)
        assertEquals(true, metrics.statusSummary.isEmpty())
    }
}
