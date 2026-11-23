package com.example.perutours.ui.screens.agent

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.perutours.data.model.TravelRequestStatus
import com.example.perutours.ui.components.*
import com.example.perutours.ui.screens.sales.components.QuotationStatusBarChart
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentHomeScreen(
    onViewRequests: () -> Unit,
    onLogout: () -> Unit = {},
    onViewPayments: () -> Unit = {},
    onManageTrips: () -> Unit = {},
    viewModel: AgentHomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogout by remember { mutableStateOf(false) }
    RequestAgentNotifications()
    if (showLogout) AlertDialog(onDismissRequest = { showLogout = false },
        title = { Text("Cerrar sesión") }, text = { Text("Tendrás que ingresar nuevamente para atender tus expedientes.") },
        confirmButton = { TextButton(onClick = { showLogout = false; onLogout() }) { Text("Cerrar sesión") } },
        dismissButton = { TextButton(onClick = { showLogout = false }) { Text("Cancelar") } })

    Scaffold(topBar = {
        TravelTopAppBar(title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AgentAvatar(uiState.agentName, uiState.photoUrl)
                Text("Mi espacio de trabajo", style = MaterialTheme.typography.titleMedium)
            }
        }, actions = {
            IconButton(onClick = viewModel::refresh, enabled = !uiState.isRefreshing) { Icon(Icons.Default.Refresh, "Actualizar mi cartera") }
            IconButton(onClick = { showLogout = true }) { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Cerrar sesión") }
        })
    }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 1040.dp).fillMaxSize(), contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                item {
                    DashboardIntro("ATENCIÓN AL VIAJERO", "Hola, ${uiState.agentName.substringBefore(' ')}",
                        "Organiza tus solicitudes, revisa comprobantes y prepara las próximas salidas.",
                        Icons.Default.SupportAgent, uiState.updatedAtMillis)
                }
                if (uiState.isRefreshing) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                uiState.errorMessage?.let { message -> item { DashboardNotice(message, viewModel::refresh, isError = true) } }
                item {
                    val loading = uiState.isLoadingCounts || uiState.updatedAtMillis <= 0
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DashboardMetric("Por cotizar", if (loading) "—" else "${uiState.pendingCount}",
                                "Solicitudes disponibles", Icons.Default.RequestQuote, Modifier.weight(1f).fillMaxHeight(), onViewRequests)
                            DashboardMetric("Cambios solicitados", if (loading) "—" else "${uiState.changesRequested}",
                                "Propuestas por revisar", Icons.Default.RateReview, Modifier.weight(1f).fillMaxHeight(), onViewRequests)
                        }
                        Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DashboardMetric("Comprobantes", if (loading) "—" else "${uiState.pendingPayments}",
                                "Pendientes de revisión", Icons.Default.AccountBalanceWallet, Modifier.weight(1f).fillMaxHeight(), onViewPayments)
                            DashboardMetric("Próximas salidas", if (loading) "—" else "${uiState.upcomingTrips.size}",
                                "Viajes pagados asignados", Icons.Default.FlightTakeoff, Modifier.weight(1f).fillMaxHeight(), onManageTrips)
                        }
                    }
                }
                if (uiState.updatedAtMillis > 0L) {
                    item {
                        DashboardSection("Solicitudes por atender", "Primero las propuestas con cambios y luego las solicitudes más antiguas.") {
                            if (uiState.actionRequests.isEmpty()) {
                                Text(if (uiState.isLoadingCounts) "Consultando solicitudes…" else "No tienes solicitudes pendientes de atención.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else uiState.actionRequests.forEach { request ->
                                val observed = request.workflowStatus == TravelRequestStatus.CHANGES_REQUESTED
                                DashboardAction(request.destination,
                                    "${request.workflowStatus.label} · ${request.travelerCount} ${if (request.travelerCount == 1) "viajero" else "viajeros"} · ${dashboardDate(request.departureAtMillis, "d MMM")}",
                                    if (observed) Icons.Default.RateReview else Icons.Default.RequestQuote, onViewRequests)
                            }
                            OutlinedButton(onClick = onViewRequests, modifier = Modifier.fillMaxWidth()) { Text("Abrir bandeja de solicitudes") }
                        }
                    }
                    item {
                        DashboardSection("Próximos viajes", "Salidas confirmadas que necesitan itinerario y seguimiento.") {
                            if (uiState.upcomingTrips.isEmpty()) {
                                Text(if (uiState.isLoadingCounts) "Consultando viajes…" else "No hay próximas salidas pagadas asignadas a tu cuenta.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else uiState.upcomingTrips.take(3).forEach { trip ->
                                DashboardAction(trip.destination,
                                    "${dashboardDate(trip.departureAtMillis, "d MMM, HH:mm")} · ${trip.travelerCount} ${if (trip.travelerCount == 1) "viajero" else "viajeros"}",
                                    Icons.Default.FlightTakeoff, onManageTrips)
                            }
                            OutlinedButton(onClick = onManageTrips, modifier = Modifier.fillMaxWidth()) { Text("Gestionar mis viajes") }
                        }
                    }
                    item {
                        DashboardAction("Revisar comprobantes", "Comprueba los datos del voucher y confirma el abono.",
                            Icons.Default.AccountBalanceWallet, onViewPayments, count = uiState.pendingPayments)
                    }
                    item {
                        DashboardSection("Distribución de mi cartera", "Solicitudes disponibles y expedientes asignados a tu cuenta.") {
                            QuotationStatusBarChart(uiState.statuses, seriesLabel = "Solicitudes",
                                emptyMessage = "Aún no hay solicitudes para comparar.")
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AgentAvatar(name: String, photoUrl: String) {
    if (photoUrl.isNotBlank()) {
        AsyncImage(photoUrl, "Foto del agente", Modifier.size(36.dp).clip(CircleShape), contentScale = ContentScale.Crop)
    } else Surface(Modifier.size(36.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
        Box(contentAlignment = Alignment.Center) {
            Text(name.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun RequestAgentNotifications() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
