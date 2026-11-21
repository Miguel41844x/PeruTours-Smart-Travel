package com.example.perutours.ui.screens.agent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.example.perutours.ui.theme.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.example.perutours.data.model.TravelRequest
import com.example.perutours.data.model.TravelRequestStatus
import com.example.perutours.ui.components.PeruToursTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class RequestFilter(val label: String) {
    PENDING("Pendientes"),
    QUOTED("Cotizadas"),
    ACCEPTED("Aceptadas"),
    OBSERVED("Observadas"),
    ALL("Todas")
}

@Composable
fun AgentRequestsScreen(
    onNavigateBack: () -> Unit,
    onRequestSelected: (String) -> Unit,
    onQuotationSelected: (String) -> Unit,
    viewModel: AgentRequestsViewModel = koinViewModel()
) {
    val requests by viewModel.requests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var selectedFilter by remember { mutableStateOf(RequestFilter.PENDING) }
    val pendingCount = remember(requests) {
        requests.count { it.workflowStatus == TravelRequestStatus.SUBMITTED }
    }
    val quotedCount = remember(requests) {
        requests.count { it.workflowStatus == TravelRequestStatus.QUOTED }
    }
    val acceptedCount = remember(requests) {
        requests.count { it.workflowStatus == TravelRequestStatus.ACCEPTED }
    }
    val observedCount = remember(requests) {
        requests.count { it.workflowStatus == TravelRequestStatus.CHANGES_REQUESTED }
    }
    val filteredRequests = remember(requests, selectedFilter) {
        when (selectedFilter) {
            RequestFilter.PENDING -> requests.filter {
                it.workflowStatus == TravelRequestStatus.SUBMITTED
            }
            RequestFilter.QUOTED -> requests.filter {
                it.workflowStatus == TravelRequestStatus.QUOTED
            }
            RequestFilter.ACCEPTED -> requests.filter {
                it.workflowStatus == TravelRequestStatus.ACCEPTED
            }
            RequestFilter.OBSERVED -> requests.filter {
                it.workflowStatus == TravelRequestStatus.CHANGES_REQUESTED
            }
            RequestFilter.ALL -> requests
        }
    }

    Scaffold(
        topBar = {
            PeruToursTopBar(
                title = "Solicitudes",
                onBack = onNavigateBack,
                actions = {
                    IconButton(onClick = viewModel::loadRequests) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar solicitudes")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RequestFilter.entries.forEach { filter ->
                    val count = when (filter) {
                        RequestFilter.PENDING -> pendingCount
                        RequestFilter.QUOTED -> quotedCount
                        RequestFilter.ACCEPTED -> acceptedCount
                        RequestFilter.OBSERVED -> observedCount
                        RequestFilter.ALL -> requests.size
                    }
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text("${filter.label} ($count)") },
                        colors = getFilterColors(filter, selectedFilter == filter),
                        leadingIcon = if (selectedFilter == filter) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }

            when {
                isLoading -> RequestsLoading()
                error != null -> RequestsError(
                    message = error.orEmpty(),
                    onRetry = viewModel::loadRequests
                )
                filteredRequests.isEmpty() -> RequestsEmpty(filter = selectedFilter)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = filteredRequests, key = TravelRequest::id) { request ->
                        TravelRequestCard(
                            request = request,
                            onClick = {
                                viewModel.openRequest(
                                    request = request,
                                    onNavigateToCreate = onRequestSelected,
                                    onNavigateToDetail = onQuotationSelected
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 900.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestsLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(
                "Cargando solicitudes...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RequestsError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No pudimos cargar las solicitudes", style = MaterialTheme.typography.titleMedium)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
        )
        Button(onClick = onRetry) { Text("Intentar de nuevo") }
    }
}

@Composable
private fun RequestsEmpty(filter: RequestFilter) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.large
        ) {
            Icon(
                Icons.Default.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(18.dp).size(36.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("No hay ${filter.label.lowercase()}", style = MaterialTheme.typography.titleMedium)
        Text(
            text = if (filter == RequestFilter.PENDING) {
                "Todas las solicitudes recibidas ya fueron atendidas."
            } else {
                "Las solicitudes aparecerán aquí cuando estén disponibles."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun TravelRequestCard(
    request: TravelRequest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPending = request.workflowStatus == TravelRequestStatus.SUBMITTED
    val dateFormat = remember {
        SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-PE"))
    }
    val departure = request.departureAtMillis.takeIf { it > 0L }
        ?.let { dateFormat.format(Date(it)) }
        ?: "Por definir"

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isPending) 2.dp else 1.dp,
            color = if (isPending) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        request.destination,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Desde ${request.originCity.ifBlank { "origen no especificado" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RequestStatus(status = request.status)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RequestFact(
                    icon = { Icon(Icons.Default.FlightTakeoff, contentDescription = null) },
                    text = departure
                )
                RequestFact(
                    icon = { Icon(Icons.Default.Group, contentDescription = null) },
                    text = "${request.travelerCount} ${if (request.travelerCount == 1) "viajero" else "viajeros"}"
                )
            }

            if (request.notes.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(modifier = Modifier.padding(10.dp)) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            request.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (request.isPackageRequest && request.packageSnapshot.isPresent) {
                Text(
                    text = "Paquete: ${request.packageSnapshot.title}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (request.assignedAgentId.isNotBlank()) {
                Text(
                    text = "Asignada a ti",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Text(
                text = "Solicitud #${request.id.takeLast(6).uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                colors = if (isPending) {
                    androidx.compose.material3.ButtonDefaults.buttonColors()
                } else {
                    androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                }
            ) {
                Text(
                    if (isPending && request.assignedAgentId.isBlank()) {
                        "Tomar y cotizar"
                    } else if (isPending) {
                        "Continuar cotización"
                    } else {
                        "Ver cotización"
                    }
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun RequestFact(icon: @Composable () -> Unit, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun getFilterColors(filter: RequestFilter, selected: Boolean) = run {
    val (container, content) = when (filter) {
        RequestFilter.PENDING -> PeruMaizeContainer to StatusPending // Ámbar
        RequestFilter.QUOTED -> PeruValleyContainer to StatusReview // Azul
        RequestFilter.ACCEPTED -> PeruValleyContainer to StatusApproved // Verde
        RequestFilter.OBSERVED -> PeruMaizeContainer to StatusPending // Naranja
        RequestFilter.ALL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    FilterChipDefaults.filterChipColors(
        containerColor = container.copy(alpha = if (selected) 1f else 0.4f),
        labelColor = content,
        selectedContainerColor = container,
        selectedLabelColor = content,
        selectedLeadingIconColor = content
    )
}

@Composable
private fun RequestStatus(status: String) {
    val workflowStatus = TravelRequestStatus.fromWire(status)
    val (container, content) = when (workflowStatus) {
        TravelRequestStatus.SUBMITTED ->
            PeruMaizeContainer to StatusPending
        TravelRequestStatus.ACCEPTED ->
            PeruValleyContainer to StatusApproved
        TravelRequestStatus.CHANGES_REQUESTED ->
            PeruMaizeContainer to StatusPending
        TravelRequestStatus.CANCELLED ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        else ->
            PeruValleyContainer to StatusReview // Azul
    }
    val label = workflowStatus.takeIf { it != TravelRequestStatus.UNKNOWN }?.label ?: status

    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}
