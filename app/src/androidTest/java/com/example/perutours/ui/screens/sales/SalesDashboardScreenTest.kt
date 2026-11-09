package com.example.perutours.ui.screens.sales

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.perutours.data.model.SalesAnalytics
import com.example.perutours.data.repository.SalesDashboardDataSource
import com.example.perutours.domain.analytics.ApprovedSaleRecord
import com.example.perutours.domain.analytics.SalesAnalyticsCalculator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SalesDashboardScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun displaysSalesAndChartsInDarkModeWithOnlyOneRequestedDestination() {
        val now = System.currentTimeMillis()
        val analytics = SalesAnalyticsCalculator().calculate(
            quotationStatuses = listOf("Aceptada", "Cotizado"),
            approvedSales = listOf(ApprovedSaleRecord(250.0, "PEN", now)),
            requestedDestinations = listOf("Cusco, Perú"),
            nowMillis = now
        )
        showDashboard(analytics)

        compose.onNodeWithText("50%").assertIsDisplayed()
        val dashboard = compose.onNode(hasScrollAction())
        dashboard.performScrollToNode(hasText("Cotizaciones por estado"))
        compose.onNodeWithText("Cotizaciones por estado").assertIsDisplayed()
        dashboard.performScrollToNode(hasText("Ingresos aprobados"))
        compose.onNodeWithText("Ingresos aprobados").assertIsDisplayed()
        dashboard.performScrollToNode(hasText("Cusco, Perú"))
        compose.onNodeWithText("Cusco, Perú").assertIsDisplayed()
    }

    @Test
    fun showsAnEmptyRevenueStateWhenTheSixMonthsHaveNoPayments() {
        val analytics = SalesAnalyticsCalculator().calculate(
            quotationStatuses = listOf("Cotizado"),
            approvedSales = emptyList(),
            requestedDestinations = emptyList()
        )
        showDashboard(analytics)

        val emptyRevenue = "Aún no hay ingresos para este periodo."
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(emptyRevenue))
        compose.onNodeWithText(emptyRevenue).assertIsDisplayed()
    }

    private fun showDashboard(analytics: SalesAnalytics) {
        val viewModel = SalesDashboardViewModel(
            object : SalesDashboardDataSource {
                override suspend fun fetchSalesAnalytics() = analytics
            }
        )
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                SalesDashboardScreen(onLogout = {}, viewModel = viewModel)
            }
        }
    }
}
