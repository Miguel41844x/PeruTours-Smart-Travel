package com.example.perutours.ui.screens.trip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.model.TripDomainRules
import com.example.perutours.data.model.TripIncident
import com.example.perutours.data.model.TripItineraryItem
import com.example.perutours.ui.components.PeruToursTopBar
import com.example.perutours.ui.theme.StatusApproved
import com.example.perutours.ui.theme.StatusCancelled
import com.example.perutours.ui.theme.StatusPending
import com.example.perutours.ui.theme.StatusReview
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AgentTripSection(val label: String) {
    ITINERARY("Itinerario"),
    INCIDENTS("Asistencia")
}

@Composable
fun AgentTripManagementScreen(
    reservationId: String,
    onNavigateBack: () -> Unit,
    viewModel: AgentTripViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedSection by remember { mutableStateOf(AgentTripSection.ITINERARY) }

    LaunchedEffect(reservationId) { viewModel.loadTrip(reservationId) }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = { PeruToursTopBar("Gestionar viaje", onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val reservation = state.selectedTrip
        if (reservation == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (state.isLoading) CircularProgressIndicator()
                else Text("No se pudo abrir el viaje.")
            }
        } else {
            AgentTripManagementContent(
                reservation = reservation,
                itinerary = state.itinerary,
                incidents = state.incidents,
                selectedSection = selectedSection,
                onSectionSelected = { selectedSection = it },
                isSaving = state.isSaving,
                itemInProgress = state.itemInProgress,
                onSaveItinerary = viewModel::saveItinerary,
                onDeleteItinerary = viewModel::deleteItinerary,
                onRespondIncident = viewModel::respondToIncident,
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 28.dp
                )
            )
        }
    }
}

@Composable
private fun AgentTripManagementContent(
    reservation: Reservation,
    itinerary: List<TripItineraryItem>,
    incidents: List<TripIncident>,
    selectedSection: AgentTripSection,
    onSectionSelected: (AgentTripSection) -> Unit,
    isSaving: Boolean,
    itemInProgress: String?,
    onSaveItinerary: (AgentItineraryDraft, TripItineraryItem?) -> Unit,
    onDeleteItinerary: (String) -> Unit,
    onRespondIncident: (String, String, String) -> Unit,
    contentPadding: PaddingValues
) {
    var editedItem by remember { mutableStateOf<TripItineraryItem?>(null) }
    var showItineraryDialog by remember { mutableStateOf(false) }

    if (showItineraryDialog) {
        AgentItineraryDialog(
            reservation = reservation,
            existing = editedItem,
            isSaving = isSaving,
            onDismiss = {
                if (!isSaving) {
                    showItineraryDialog = false
                    editedItem = null
                }
            },
            onConfirm = { draft ->
                onSaveItinerary(draft, editedItem)
                showItineraryDialog = false
                editedItem = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { AgentTripHeader(reservation) }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AgentTripSection.entries.forEach { section ->
                    FilterChip(
                        selected = selectedSection == section,
                        onClick = { onSectionSelected(section) },
                        label = {
                            Text(
                                if (section == AgentTripSection.INCIDENTS) {
                                    "${section.label} (${incidents.count { it.status != TripIncident.STATUS_CLOSED }})"
                                } else {
                                    section.label
                                }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (selectedSection == AgentTripSection.ITINERARY) {
            item {
                Button(
                    onClick = {
                        editedItem = null
                        showItineraryDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Agregar actividad")
                }
            }
            if (itinerary.isEmpty()) {
                item { AgentEmptySection("Todavía no programaste actividades para este viaje.") }
            } else {
                val grouped = itinerary.groupBy(TripItineraryItem::dayIndex).toSortedMap()
                grouped.forEach { (day, activities) ->
                    item(key = "agent-day-$day") {
                        Text(
                            "Día $day",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 760.dp)
                                .padding(top = 6.dp)
                        )
                    }
                    items(activities, key = TripItineraryItem::id) { item ->
                        AgentItineraryCard(
                            item = item,
                            isBusy = itemInProgress == item.id,
                            onEdit = {
                                editedItem = item
                                showItineraryDialog = true
                            },
                            onDelete = { onDeleteItinerary(item.id) }
                        )
                    }
                }
            }
        } else {
            if (incidents.isEmpty()) {
                item { AgentEmptySection("El viajero no ha reportado incidencias.") }
            } else {
                items(incidents, key = TripIncident::id) { incident ->
                    AgentIncidentCard(
                        incident = incident,
                        isBusy = itemInProgress == incident.id,
                        onRespond = { response, status ->
                            onRespondIncident(incident.id, response, status)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentTripHeader(reservation: Reservation) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                reservation.destination,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "${reservation.originCity} · ${TripDomainRules.tripDurationDays(reservation)} días · " +
                    "${reservation.travelerCount} viajeros",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Salida ${formatAgentManagementDate(reservation.departureAtMillis)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun AgentItineraryCard(
    item: TripItineraryItem,
    isBusy: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.AutoMirrored.Filled.EventNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${formatAgentManagementTime(item.startAtMillis)} · ${item.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.locationName.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(item.locationName, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            IconButton(onClick = onEdit, enabled = !isBusy) {
                Icon(Icons.Default.Edit, contentDescription = "Editar ${item.title}")
            }
            IconButton(onClick = onDelete, enabled = !isBusy) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar ${item.title}")
            }
        }
    }
}

@Composable
private fun AgentIncidentCard(
    incident: TripIncident,
    isBusy: Boolean,
    onRespond: (String, String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val statusColor = when (incident.status) {
        TripIncident.STATUS_RESOLVED -> StatusApproved
        TripIncident.STATUS_IN_REVIEW -> StatusReview
        TripIncident.STATUS_CLOSED -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> StatusPending
    }

    if (showDialog) {
        AgentIncidentResponseDialog(
            incident = incident,
            isSaving = isBusy,
            onDismiss = { if (!isBusy) showDialog = false },
            onConfirm = { response, status ->
                onRespond(response, status)
                showDialog = false
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(incident.category, style = MaterialTheme.typography.titleSmall)
                Surface(color = statusColor.copy(alpha = 0.12f), shape = CircleShape) {
                    Text(
                        incident.status,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
            Text(incident.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Prioridad ${incident.priority.lowercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = if (
                    incident.priority == TripIncident.PRIORITY_HIGH ||
                    incident.priority == TripIncident.PRIORITY_URGENT
                ) StatusCancelled else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (incident.agentResponse.isNotBlank()) {
                Text(
                    "Respuesta: ${incident.agentResponse}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (incident.status != TripIncident.STATUS_CLOSED) {
                Button(
                    onClick = { showDialog = true },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (incident.agentResponse.isBlank()) "Responder" else "Actualizar respuesta")
                }
            }
        }
    }
}

@Composable
private fun AgentItineraryDialog(
    reservation: Reservation,
    existing: TripItineraryItem?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (AgentItineraryDraft) -> Unit
) {
    var draft by remember(existing?.id) {
        mutableStateOf(existing?.let(AgentItineraryForm::fromModel) ?: AgentItineraryDraft())
    }
    val validationError = AgentItineraryForm.validate(draft, reservation)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Nueva actividad" else "Editar actividad") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = draft.title,
                        onValueChange = { draft = draft.copy(title = it.take(120)) },
                        label = { Text("Título") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = draft.description,
                        onValueChange = { draft = draft.copy(description = it.take(1_500)) },
                        label = { Text("Descripción") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("Tipo de actividad", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        TripItineraryItem.VALID_CATEGORIES.forEach { category ->
                            FilterChip(
                                selected = draft.category == category,
                                onClick = { draft = draft.copy(category = category) },
                                label = { Text(category) }
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = draft.dayText,
                            onValueChange = { draft = draft.copy(dayText = it.filter(Char::isDigit).take(3)) },
                            label = { Text("Día") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = draft.timeText,
                            onValueChange = { draft = draft.copy(timeText = it.take(5)) },
                            label = { Text("Hora HH:mm") },
                            singleLine = true,
                            modifier = Modifier.weight(1.4f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = draft.durationMinutesText,
                        onValueChange = {
                            draft = draft.copy(durationMinutesText = it.filter(Char::isDigit).take(4))
                        },
                        label = { Text("Duración en minutos") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = draft.locationName,
                        onValueChange = { draft = draft.copy(locationName = it.take(160)) },
                        label = { Text("Punto de encuentro") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = draft.providerName,
                        onValueChange = { draft = draft.copy(providerName = it.take(120)) },
                        label = { Text("Proveedor u operador") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = draft.notes,
                        onValueChange = { draft = draft.copy(notes = it.take(500)) },
                        label = { Text("Notas para el viajero") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (validationError != null) {
                    item {
                        Text(
                            validationError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(draft) },
                enabled = validationError == null && !isSaving
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancelar") }
        }
    )
}

@Composable
private fun AgentIncidentResponseDialog(
    incident: TripIncident,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var response by remember { mutableStateOf(incident.agentResponse) }
    var status by remember {
        mutableStateOf(
            incident.status.takeIf {
                it == TripIncident.STATUS_IN_REVIEW || it == TripIncident.STATUS_RESOLVED
            } ?: TripIncident.STATUS_IN_REVIEW
        )
    }
    val validationError = TripDomainRules.validateAgentResponse(response)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Responder incidencia") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(incident.description, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = response,
                    onValueChange = { response = it.take(1_000) },
                    label = { Text("Respuesta para el viajero") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        TripIncident.STATUS_IN_REVIEW,
                        TripIncident.STATUS_RESOLVED
                    ).forEach { option ->
                        FilterChip(
                            selected = status == option,
                            onClick = { status = option },
                            label = { Text(option) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(response.trim(), status) },
                enabled = validationError == null && !isSaving
            ) { Text("Enviar respuesta") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancelar") }
        }
    )
}

@Composable
private fun AgentEmptySection(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp)
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(44.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatAgentManagementDate(value: Long): String = if (value <= 0L) {
    "por confirmar"
} else {
    SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-PE")).format(Date(value))
}

private fun formatAgentManagementTime(value: Long): String = if (value <= 0L) {
    "Sin hora"
} else {
    SimpleDateFormat("HH:mm", Locale.forLanguageTag("es-PE")).format(Date(value))
}
