package com.example.perutours.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.perutours.data.model.*
import com.example.perutours.ui.components.*
import com.example.perutours.ui.screens.admin.components.CatalogCoverageChart
import com.example.perutours.ui.screens.admin.components.CatalogPublicationChart
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onManageDestinations: () -> Unit,
    onManagePackages: () -> Unit,
    onManageProviders: () -> Unit,
    onManageServices: () -> Unit,
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: AdminDashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val metrics = uiState.metrics
    var showLogout by remember { mutableStateOf(false) }
    var showReservations by rememberSaveable { mutableStateOf(false) }
    val openArea: (CatalogArea) -> Unit = { area ->
        when (area) {
            CatalogArea.PROVIDERS -> onManageProviders()
            CatalogArea.SERVICES -> onManageServices()
            CatalogArea.DESTINATIONS -> onManageDestinations()
            CatalogArea.PACKAGES -> onManagePackages()
        }
    }
    if (showLogout) {
        AlertDialog(onDismissRequest = { showLogout = false }, title = { Text("Cerrar sesión") },
            text = { Text("Tendrás que ingresar nuevamente para administrar el catálogo.") },
            confirmButton = { TextButton(onClick = { showLogout = false; onLogout() }) { Text("Cerrar sesión") } },
            dismissButton = { TextButton(onClick = { showLogout = false }) { Text("Cancelar") } })
    }
    Scaffold(topBar = {
        TravelTopAppBar(title = { Text("Administración") }, navigationIcon = {
            IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
        }, actions = {
            IconButton(onClick = viewModel::refresh, enabled = !uiState.isLoading) {
                Icon(Icons.Default.Refresh, "Actualizar catálogo")
            }
            IconButton(onClick = { showLogout = true }) { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Cerrar sesión") }
        })
    }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 1040.dp).fillMaxSize(),
                contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                item {
                    DashboardIntro("CONTROL DEL CATÁLOGO", "Gestiona la oferta turística",
                        "Proveedores, servicios y publicaciones en un solo lugar.", Icons.Default.Inventory2, metrics.updatedAtMillis)
                }
                if (uiState.isLoadingMetrics) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                uiState.errorMessage?.let { message -> item { DashboardNotice(message, viewModel::refresh, isError = true) } }
                if (metrics.isCached) item {
                    DashboardNotice("Sin conexión. Mostrando el catálogo guardado el ${uiState.formattedUpdatedAt}.")
                }
                if (metrics.hasData) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                DashboardMetric("Proveedores activos", if (metrics.hasData) "${metrics.activeProviders}" else "—",
                                    "de ${metrics.totalProviders} registrados", Icons.Default.Business, Modifier.weight(1f).fillMaxHeight(), onManageProviders)
                                DashboardMetric("Servicios disponibles", if (metrics.hasData) "${metrics.availableServices}" else "—",
                                    "de ${metrics.totalServices} en catálogo", Icons.Default.RoomService, Modifier.weight(1f).fillMaxHeight(), onManageServices)
                            }
                            Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                DashboardMetric("Destinos publicados", if (metrics.hasData) "${metrics.publishedDestinations}" else "—",
                                    "de ${metrics.totalDestinations} destinos", Icons.Default.Place, Modifier.weight(1f).fillMaxHeight(), onManageDestinations)
                                DashboardMetric("Paquetes publicados", if (metrics.hasData) "${metrics.publishedPackages}" else "—",
                                    "de ${metrics.totalPackages} paquetes", Icons.Default.Luggage, Modifier.weight(1f).fillMaxHeight(), onManagePackages)
                            }
                        }
                    }
                    if (metrics.hasData) item {
                        DashboardSection("Pendientes del catálogo", "Prioriza los datos que necesitan atención antes de vender.") {
                            if (metrics.alerts.isEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary)
                                    Text("No se detectaron pendientes en el catálogo actual.", style = MaterialTheme.typography.bodyMedium)
                                }
                            } else metrics.alerts.forEach { alert ->
                                DashboardAction(alert.title, "Abrir la sección para revisar los registros.", Icons.AutoMirrored.Filled.FactCheck,
                                    onClick = { openArea(alert.area) }, count = alert.count)
                            }
                        }
                    }
                    item {
                        DashboardSection("Cobertura de servicios", "Servicios con tarifa, proveedor activo y destino publicado. Hasta seis destinos.") {
                            CatalogCoverageChart(metrics.servicesByDestination)
                        }
                    }
                    item {
                        DashboardSection("Publicación de paquetes", "Una vista de los paquetes que están en venta, en preparación o archivados.") {
                            CatalogPublicationChart(metrics.packagesByStatus)
                        }
                    }
                    item {
                        DashboardSection("Perfiles registrados", "${metrics.totalUsers} perfiles en la plataforma, agrupados por su rol.") {
                            if (metrics.usersByRole.isEmpty()) {
                                Text("Aún no hay perfiles para mostrar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else UserRole.entries.forEach { role ->
                                val count = metrics.usersByRole[role.name] ?: 0
                                if (count > 0) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (role == UserRole.UNKNOWN) "Sin rol reconocido" else role.label,
                                        style = MaterialTheme.typography.bodyMedium)
                                    Text(count.toString(), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                item {
                    DashboardSection("Gestionar la oferta", "Mantén los datos que utilizan los agentes al cotizar.") {
                        DashboardAction("Proveedores", "Hoteles, transportistas, guías y contactos.", Icons.Default.Business, onManageProviders)
                        DashboardAction("Servicios", "Tarifas, disponibilidad y relación con cada destino.", Icons.Default.RoomService, onManageServices)
                        DashboardAction("Destinos", "Información turística y publicaciones.", Icons.Default.Place, onManageDestinations)
                        DashboardAction("Paquetes", "Servicios incluidos y condiciones de venta.", Icons.Default.Luggage, onManagePackages)
                    }
                }
                item {
                    OutlinedButton(onClick = { showReservations = !showReservations }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showReservations) "Ocultar consulta de reservas" else "Consultar reservas")
                    }
                }
                if (showReservations) {
                    item {
                        Text("Consulta de reservas", style = MaterialTheme.typography.titleLarge)
                        Text("Supervisa expedientes sin modificar sus estados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(AdminDashboardConstants.AVAILABLE_FILTERS) { filter ->
                                FilterChip(selected = uiState.selectedFilterStatus == filter,
                                    onClick = { viewModel.setFilterStatus(filter) }, label = { Text(filter) })
                            }
                        }
                    }
                    if (uiState.isLoadingReservations) {
                        item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    } else if (uiState.reservations.isEmpty()) {
                        item { Text("No hay reservas con este filtro.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(uiState.reservations, key = { it.id }) { reservation -> ReservationSummary(reservation) }
                        if (!uiState.endReached) item {
                            OutlinedButton(onClick = viewModel::loadMoreReservations, enabled = !uiState.isLoadingMore,
                                modifier = Modifier.fillMaxWidth()) {
                                Text(if (uiState.isLoadingMore) "Cargando…" else "Cargar más reservas")
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun ReservationSummary(reservation: Reservation) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(reservation.destination, style = MaterialTheme.typography.titleMedium)
            Text(reservation.workflowStatus.label, color = MaterialTheme.colorScheme.primary)
            Text("${reservation.travelerCount} viajeros · ${dashboardDate(reservation.departureAtMillis, "d MMM yyyy")}",
                style = MaterialTheme.typography.bodyMedium)
            Text(dashboardMoney(reservation.totalAmount, reservation.currency), style = MaterialTheme.typography.labelLarge)
        }
    }
}
