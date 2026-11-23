package com.example.perutours.ui.screens.sales

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.perutours.data.model.CurrencyRevenue
import com.example.perutours.ui.components.*
import com.example.perutours.ui.screens.sales.components.DestinationDemandBarChart
import com.example.perutours.ui.screens.sales.components.DestinationRanking
import com.example.perutours.ui.screens.sales.components.MonthlyRevenueLineChart
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesDashboardScreen(onLogout: () -> Unit, viewModel: SalesDashboardViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedCurrency by rememberSaveable { mutableStateOf("PEN") }
    var showLogout by remember { mutableStateOf(false) }
    val analytics = uiState.analytics
    val currencies = analytics.revenueByCurrency
    val revenue = currencies.firstOrNull { it.currency == selectedCurrency }
        ?: currencies.firstOrNull() ?: CurrencyRevenue()
    if (showLogout) AlertDialog(onDismissRequest = { showLogout = false }, title = { Text("Cerrar sesión") },
        text = { Text("Tendrás que ingresar nuevamente para consultar los resultados.") },
        confirmButton = { TextButton(onClick = { showLogout = false; onLogout() }) { Text("Cerrar sesión") } },
        dismissButton = { TextButton(onClick = { showLogout = false }) { Text("Cancelar") } })

    Scaffold(topBar = {
        TravelTopAppBar(title = { Text("Resultados del negocio") }, actions = {
            IconButton(onClick = viewModel::refresh, enabled = !uiState.isLoading) { Icon(Icons.Default.Refresh, "Actualizar estadísticas") }
            IconButton(onClick = { showLogout = true }) { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Cerrar sesión") }
        })
    }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 1040.dp).fillMaxSize(), contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                item {
                    DashboardIntro("PANEL GERENCIAL", "Resultados económicos",
                        "Cobros, costos cotizados y oportunidades comerciales para tomar decisiones.",
                        Icons.AutoMirrored.Filled.TrendingUp, analytics.updatedAtMillis)
                }
                if (uiState.isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                uiState.errorMessage?.let { message -> item { DashboardNotice(message, viewModel::refresh, isError = true) } }
                if (analytics.updatedAtMillis > 0L) {
                    if (currencies.size > 1) item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Moneda de análisis", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                currencies.forEach { currency ->
                                    FilterChip(selected = revenue.currency == currency.currency,
                                        onClick = { selectedCurrency = currency.currency }, label = { Text(currency.currency) })
                                }
                            }
                        }
                    }
                    item {
                        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Cobros aprobados", style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer)
                                Text(if (uiState.isLoading && !analytics.hasData) "—" else dashboardMoney(revenue.totalAmount, revenue.currency),
                                    modifier = Modifier.testTag("approved-revenue-value"),
                                    style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer)
                                Text("Acumulado histórico · ${revenue.approvedSales} ventas en ${revenue.currency}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                DashboardMetric("Margen bruto estimado",
                                    if (revenue.costedSales > 0) dashboardMoney(revenue.estimatedGrossProfit, revenue.currency) else "—",
                                    "Sobre ${revenue.costedSales} ${if (revenue.costedSales == 1) "venta" else "ventas"} con costos", Icons.Default.AccountBalanceWallet, Modifier.weight(1f).fillMaxHeight())
                                DashboardMetric("Margen sobre ventas",
                                    revenue.estimatedMarginPercent?.let { String.format(Locale.ROOT, "%.1f%%", it) } ?: "—",
                                    "Solo ventas con costo válido", Icons.Default.Percent, Modifier.weight(1f).fillMaxHeight())
                            }
                            Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                DashboardMetric("Ticket promedio",
                                    dashboardMoney(if (revenue.approvedSales > 0) revenue.totalAmount / revenue.approvedSales else 0.0, revenue.currency),
                                    "Por venta aprobada", Icons.Default.Payments, Modifier.weight(1f).fillMaxHeight())
                                DashboardMetric("Ventas aprobadas", revenue.approvedSales.toString(),
                                    "Pagos confirmados en ${revenue.currency}", Icons.Default.CheckCircle, Modifier.weight(1f).fillMaxHeight())
                            }
                        }
                    }
                    item {
                        DashboardSection("Ingresos aprobados", "Pagos confirmados en ${revenue.currency} durante los últimos seis meses.") {
                            MonthlyRevenueLineChart(revenue.monthlyRevenue, revenue.currency,
                                estimatedProfit = if (revenue.costedSales > 0) revenue.monthlyEstimatedProfit else emptyList())
                        }
                    }
                    item {
                        DashboardSection("Costos y margen estimado", "Comparación de ventas aprobadas con el costo de sus cotizaciones.") {
                            if (revenue.costedSales == 0) {
                                Text("Aún no hay ventas con costos válidos para calcular el margen.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                FinancialRow("Cobros con costo disponible", dashboardMoney(revenue.costedRevenue, revenue.currency))
                                FinancialRow("Costo presupuestado de proveedores", dashboardMoney(revenue.estimatedSupplierCost, revenue.currency))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                FinancialRow("Margen bruto estimado", dashboardMoney(revenue.estimatedGrossProfit, revenue.currency))
                            }
                            if (revenue.approvedSales > revenue.costedSales) Text(
                                "${revenue.approvedSales - revenue.costedSales} ${if (revenue.approvedSales - revenue.costedSales == 1) "venta sin costo válido se excluye" else "ventas sin costo válido se excluyen"} del cálculo del margen.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("El margen es una estimación sobre costos cotizados. No incluye gastos operativos ni representa utilidad neta.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item {
                        DashboardSection("Eficacia comercial", "Conversión histórica de cotizaciones a ventas con pago aprobado, en todas las monedas.") {
                            Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                DashboardMetric("Cotizaciones emitidas", analytics.totalQuotations.toString(),
                                    "Histórico de propuestas", Icons.Default.RequestQuote, Modifier.weight(1f).fillMaxHeight())
                                DashboardMetric("Conversión a venta", "${analytics.conversionRate.toInt()}%",
                                    "${analytics.approvedSales} ventas aprobadas", Icons.Default.Assessment, Modifier.weight(1f).fillMaxHeight())
                            }
                        }
                    }
                    item {
                        DashboardSection("Destinos más solicitados", "Demanda registrada para orientar la oferta y las próximas campañas.") {
                            DestinationDemandBarChart(analytics.topDestinations)
                            DestinationRanking(analytics.topDestinations)
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun FinancialRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}
