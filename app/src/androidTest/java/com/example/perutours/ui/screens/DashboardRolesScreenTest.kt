package com.example.perutours.ui.screens

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.perutours.data.model.*
import com.example.perutours.data.repository.*
import com.example.perutours.ui.screens.admin.AdminDashboardScreen
import com.example.perutours.ui.screens.admin.AdminDashboardViewModel
import com.example.perutours.ui.screens.agent.AgentHomeScreen
import com.example.perutours.ui.screens.agent.AgentHomeViewModel
import com.example.perutours.ui.screens.sales.SalesDashboardScreen
import com.example.perutours.ui.screens.sales.SalesDashboardViewModel
import com.example.perutours.domain.analytics.ApprovedSaleRecord
import com.example.perutours.domain.analytics.SalesAnalyticsCalculator
import com.example.perutours.ui.theme.PeruToursTheme
import com.google.firebase.firestore.DocumentSnapshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DashboardRolesScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun administratorShowsCatalogAndRoleCountsWithWorkingShortcuts() {
        var openedProviders = false
        val metrics = AdminMetrics(totalProviders = 12, activeProviders = 10, totalServices = 42,
            availableServices = 36, totalDestinations = 8, publishedDestinations = 6,
            totalPackages = 18, publishedPackages = 14,
            servicesByDestination = linkedMapOf("Cusco" to 14, "Arequipa" to 10, "Chiclayo" to 8, "Lima" to 4),
            packagesByStatus = mapOf("PUBLICADO" to 14, "BORRADOR" to 3, "ARCHIVADO" to 1),
            usersByRole = mapOf("CLIENT" to 64, "AGENT" to 4, "MANAGER" to 1, "ADMIN" to 1),
            alerts = listOf(CatalogAlert(CatalogArea.PROVIDERS, "Proveedores activos sin servicios", 2)),
            updatedAtMillis = System.currentTimeMillis())
        val viewModel = AdminDashboardViewModel(object : AdminDashboardDataSource {
            override suspend fun fetchAdminMetrics() = metrics
            override suspend fun fetchReservations(limit: Long, statusFilter: String?, lastDocument: DocumentSnapshot?) =
                emptyList<Reservation>() to null
        })
        compose.setContent { PeruToursTheme(darkTheme = false) {
            AdminDashboardScreen({}, {}, { openedProviders = true }, {}, viewModel = viewModel)
        } }
        compose.onNodeWithText("Proveedores activos").assertIsDisplayed()
        capture("admin-top")
        compose.onNodeWithText("Proveedores activos").performClick()
        compose.runOnIdle { check(openedProviders) }
        scrollTo("Cobertura de servicios")
        capture("admin-coverage")
        scrollTo("Publicación de paquetes")
        capture("admin-publication")
        scrollTo("Perfiles registrados")
        compose.onNodeWithText("Perfiles registrados").assertIsDisplayed()
    }

    @Test fun managerShowsEstimatedMarginAndExcludesUncostedSales() {
        val now = System.currentTimeMillis()
        val metrics = SalesAnalyticsCalculator().calculate(listOf("Aceptada", "Aceptada", "Cotizado"),
            listOf(ApprovedSaleRecord(1200.0, "PEN", now, 900.0), ApprovedSaleRecord(500.0, "PEN", now)),
            listOf("Cusco, Perú", "Cusco, Perú", "Arequipa, Perú"), now)
        val viewModel = SalesDashboardViewModel(object : SalesDashboardDataSource {
            override suspend fun fetchSalesAnalytics() = metrics
        })
        compose.setContent { PeruToursTheme(darkTheme = true) {
            SalesDashboardScreen({}, viewModel)
        } }
        compose.onNodeWithText("Cobros aprobados").assertIsDisplayed()
        capture("manager-top-dark")
        scrollTo("Margen bruto estimado")
        compose.onAllNodesWithText("S/ 300.00")[0].assertExists()
        capture("manager-metrics-dark")
        scrollTo("Ingresos aprobados")
        capture("manager-chart-dark")
        scrollTo("1 venta sin costo válido se excluye del cálculo del margen.")
        compose.onNodeWithText("1 venta sin costo válido se excluye del cálculo del margen.").assertIsDisplayed()
    }

    @Test fun agentShowsPendingWorkAndUpcomingTripsWithLargerText() {
        val now = System.currentTimeMillis()
        val data = AgentDashboard("Miguel Sánchez", "", listOf(
            QuotationStatusMetric(TravelRequestStatus.SUBMITTED.label, 7),
            QuotationStatusMetric(TravelRequestStatus.CHANGES_REQUESTED.label, 2),
            QuotationStatusMetric(TravelRequestStatus.QUOTED.label, 5)
        ), 3, actionRequests = listOf(
            TravelRequest(id = "request", destination = "Arequipa, Perú", travelerCount = 2,
                status = "Observada", departureAtMillis = now + 86400000L)
        ), upcomingTrips = listOf(Reservation(id = "trip", destination = "Cusco, Perú",
            departureAtMillis = now + 86400000L, status = "Pagado")), updatedAtMillis = now)
        val viewModel = AgentHomeViewModel(AgentDashboardDataSource { data })
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                PeruToursTheme(darkTheme = false) { AgentHomeScreen({}, viewModel = viewModel) }
            }
        }
        compose.onNodeWithText("Hola, Miguel").assertIsDisplayed()
        capture("agent-top-large-text")
        scrollTo("Cambios solicitados")
        compose.onNodeWithText("Cambios solicitados").assertIsDisplayed()
        capture("agent-metrics-large-text")
        scrollTo("Arequipa, Perú")
        compose.onNodeWithText("Arequipa, Perú").assertIsDisplayed()
        capture("agent-work-large-text")
        scrollTo("Distribución de mi cartera")
        capture("agent-chart-large-text")
    }

    private fun scrollTo(text: String) {
        val list = compose.onNode(hasScrollToIndexAction())
        list.performScrollToNode(hasText(text))
        val top = compose.onAllNodesWithText(text)[0].fetchSemanticsNode().boundsInRoot.top
        val viewportTop = list.fetchSemanticsNode().boundsInRoot.top
        list.performSemanticsAction(SemanticsActions.ScrollBy) { scroll -> scroll(0f, top - viewportTop) }
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "dashboard-previews").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
