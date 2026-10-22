package com.example.perutours.ui.screens.trip

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.model.TripDomainRules
import com.example.perutours.data.model.TripItineraryItem
import com.example.perutours.data.model.TripOverview
import com.example.perutours.ui.components.PeruToursTopBar
import com.example.perutours.ui.theme.StatusApproved
import com.example.perutours.ui.theme.StatusReview
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun TripDetailScreen(
    reservationId: String,
    onNavigateBack: () -> Unit,
    viewModel: TripDetailViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedSection by remember { mutableStateOf(TripDetailSection.ITINERARY) }

    LaunchedEffect(reservationId) { viewModel.load(reservationId) }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            PeruToursTopBar(
                title = "Mi viaje",
                onBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = viewModel::refresh,
                        enabled = !state.isRefreshing
                    ) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = "Actualizar viaje")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val overview = state.overview
        when {
            overview != null -> TripDetailContent(
                overview = overview,
                selectedSection = selectedSection,
                onSectionSelected = { selectedSection = it },
                isSavingChecklist = state.isSavingChecklist,
                checklistItemInProgress = state.checklistItemInProgress,
                onAddChecklist = viewModel::addChecklistItem,
                onToggleChecklist = viewModel::toggleChecklistItem,
                onDeleteChecklist = viewModel::deleteChecklistItem,
                onSyncChecklist = viewModel::syncChecklist,
                isSubmittingIncident = state.isSubmittingIncident,
                isDetectingLocation = state.isDetectingLocation,
                incidentLocation = state.incidentLocation,
                onDetectIncidentLocation = viewModel::detectIncidentLocation,
                onClearIncidentLocation = viewModel::clearIncidentLocation,
                onCreateIncident = viewModel::createIncident,
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 28.dp
                )
            )
            state.isLoading || state.isRefreshing -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            else -> TripNotAvailable(
                onRetry = viewModel::refresh,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun TripDetailContent(
    overview: TripOverview,
    selectedSection: TripDetailSection,
    onSectionSelected: (TripDetailSection) -> Unit,
    isSavingChecklist: Boolean,
    checklistItemInProgress: String?,
    onAddChecklist: (String, String) -> Unit,
    onToggleChecklist: (String) -> Unit,
    onDeleteChecklist: (String) -> Unit,
    onSyncChecklist: () -> Unit,
    isSubmittingIncident: Boolean,
    isDetectingLocation: Boolean,
    incidentLocation: com.example.perutours.data.location.DetectedLocation?,
    onDetectIncidentLocation: () -> Unit,
    onClearIncidentLocation: () -> Unit,
    onCreateIncident: (String, String, String) -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { TripHeader(overview) }
        item {
            TripSectionSelector(
                selected = selectedSection,
                onSelected = onSectionSelected
            )
        }

        when (selectedSection) {
            TripDetailSection.ITINERARY -> itineraryItems(overview)
            TripDetailSection.CHECKLIST -> tripChecklistItems(
                overview = overview,
                isSaving = isSavingChecklist,
                itemInProgress = checklistItemInProgress,
                onAdd = onAddChecklist,
                onToggle = onToggleChecklist,
                onDelete = onDeleteChecklist,
                onSync = onSyncChecklist
            )
            TripDetailSection.SUPPORT -> tripSupportItems(
                overview = overview,
                isSubmitting = isSubmittingIncident,
                isDetectingLocation = isDetectingLocation,
                detectedLocation = incidentLocation,
                onDetectLocation = onDetectIncidentLocation,
                onClearLocation = onClearIncidentLocation,
                onCreateIncident = onCreateIncident
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.itineraryItems(
    overview: TripOverview
) {
    val grouped = overview.itinerary.groupBy(TripItineraryItem::dayIndex).toSortedMap()
    if (grouped.isEmpty()) {
        item {
            PendingSection(
                icon = Icons.Default.CalendarMonth,
                title = "Itinerario en preparación",
                text = "Tu agente todavía no ha programado las actividades."
            )
        }
        return
    }

    grouped.forEach { (day, dayItems) ->
        item(key = "day-$day") {
            ItineraryDayHeading(
                day = day,
                departureAtMillis = overview.reservation.departureAtMillis
            )
        }
        items(dayItems, key = TripItineraryItem::id) { activity ->
            ItineraryActivityCard(activity)
        }
    }
}

@Composable
private fun TripHeader(overview: TripOverview) {
    val reservation = overview.reservation
    val phase = TripDomainRules.tripPhase(reservation)
    val phaseColor = if (phase == TripDomainRules.PHASE_IN_PROGRESS) StatusApproved else StatusReview

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        reservation.destination,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Desde ${reservation.originCity.ifBlank { "origen por confirmar" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Surface(
                    color = phaseColor.copy(alpha = 0.14f),
                    shape = CircleShape
                ) {
                    Text(
                        phase,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = phaseColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TripHeaderMetric(
                    label = "Salida",
                    value = formatDate(reservation.departureAtMillis)
                )
                TripHeaderMetric(
                    label = "Duración",
                    value = "${TripDomainRules.tripDurationDays(reservation)} días"
                )
                TripHeaderMetric(
                    label = "Viajeros",
                    value = reservation.travelerCount.toString()
                )
            }
        }
    }
}

@Composable
private fun TripHeaderMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun TripSectionSelector(
    selected: TripDetailSection,
    onSelected: (TripDetailSection) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TripDetailSection.entries.forEach { section ->
            FilterChip(
                selected = selected == section,
                onClick = { onSelected(section) },
                label = {
                    Text(
                        section.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ItineraryDayHeading(day: Int, departureAtMillis: Long) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .padding(top = 6.dp)
    ) {
        Text("Día $day", style = MaterialTheme.typography.titleMedium)
        if (departureAtMillis > 0L) {
            val value = departureAtMillis + TimeUnit.DAYS.toMillis((day - 1).toLong())
            Text(
                formatLongDate(value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ItineraryActivityCard(item: TripItineraryItem) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    itineraryIcon(item.category),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatActivityTime(item.startAtMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (item.description.isNotBlank()) {
                    Text(
                        item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.providerName.isNotBlank()) {
                    Text(
                        "Operador: ${item.providerName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.locationName.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            item.locationName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (item.source == TripItineraryItem.SOURCE_RESERVATION) {
                    AssistChip(
                        onClick = {},
                        label = { Text(item.status) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                if (item.hasCoordinates) {
                    OutlinedButton(
                        onClick = {
                            val uri = Uri.parse(
                                "geo:${item.latitude},${item.longitude}?q=" +
                                    "${item.latitude},${item.longitude}(" +
                                    Uri.encode(item.title) + ")"
                            )
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Abrir en el mapa")
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingSection(
    icon: ImageVector,
    title: String,
    text: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TripNotAvailable(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(54.dp))
        Spacer(Modifier.height(14.dp))
        Text("Viaje no disponible", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Comprueba que la reserva esté pagada y vuelve a intentarlo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}

private fun itineraryIcon(category: String): ImageVector = when (category) {
    TripItineraryItem.CATEGORY_TRANSPORT -> Icons.Default.DirectionsBus
    TripItineraryItem.CATEGORY_LODGING -> Icons.Default.Hotel
    TripItineraryItem.CATEGORY_FOOD -> Icons.Default.Restaurant
    else -> Icons.Default.Explore
}

private fun formatDate(value: Long): String = if (value <= 0L) {
    "Por confirmar"
} else {
    SimpleDateFormat("d MMM", Locale.forLanguageTag("es-PE")).format(Date(value))
}

private fun formatLongDate(value: Long): String = SimpleDateFormat(
    "EEEE d 'de' MMMM",
    Locale.forLanguageTag("es-PE")
).format(Date(value)).replaceFirstChar { it.uppercase() }

private fun formatActivityTime(value: Long): String = if (value <= 0L) {
    "Por confirmar"
} else {
    SimpleDateFormat("HH:mm", Locale.forLanguageTag("es-PE")).format(Date(value))
}
