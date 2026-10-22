package com.example.perutours.ui.screens.trip

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.model.TripDomainRules
import com.example.perutours.ui.components.PeruToursTopBar
import com.example.perutours.ui.theme.StatusApproved
import com.example.perutours.ui.theme.StatusReview
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripListScreen(
    onNavigateBack: () -> Unit,
    onTripSelected: (String) -> Unit,
    onViewReservations: () -> Unit,
    viewModel: TripListViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            PeruToursTopBar(
                title = "Mis viajes",
                onBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = viewModel::refresh,
                        enabled = !state.isRefreshing
                    ) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualizar viajes")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when {
            state.isLoading && state.trips.isEmpty() -> TripListLoading(
                modifier = Modifier.padding(paddingValues)
            )
            state.trips.isEmpty() -> EmptyTripList(
                onViewReservations = onViewReservations,
                modifier = Modifier.padding(paddingValues)
            )
            else -> TripListContent(
                trips = state.trips,
                onTripSelected = onTripSelected,
                onViewReservations = onViewReservations,
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = paddingValues.calculateTopPadding() + 18.dp,
                    bottom = paddingValues.calculateBottomPadding() + 24.dp
                )
            )
        }
    }
}

@Composable
private fun TripListContent(
    trips: List<Reservation>,
    onTripSelected: (String) -> Unit,
    onViewReservations: () -> Unit,
    contentPadding: PaddingValues
) {
    val now = System.currentTimeMillis()
    val currentAndUpcoming = trips.filter {
        TripDomainRules.tripPhase(it, now) != TripDomainRules.PHASE_FINISHED
    }
    val finished = trips.filter {
        TripDomainRules.tripPhase(it, now) == TripDomainRules.PHASE_FINISHED
    }.sortedByDescending(Reservation::returnAtMillis)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Tu viaje, en un solo lugar",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Consulta el itinerario, prepara tu equipaje y solicita ayuda durante la ruta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (currentAndUpcoming.isNotEmpty()) {
            item { TripSectionTitle("Próximos viajes") }
            items(currentAndUpcoming, key = Reservation::id) { reservation ->
                TripCard(
                    reservation = reservation,
                    onClick = { onTripSelected(reservation.id) }
                )
            }
        }

        if (finished.isNotEmpty()) {
            item { TripSectionTitle("Viajes anteriores") }
            items(finished, key = Reservation::id) { reservation ->
                TripCard(
                    reservation = reservation,
                    onClick = { onTripSelected(reservation.id) }
                )
            }
        }

        item {
            OutlinedButton(
                onClick = onViewReservations,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Luggage, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ver todas mis reservas")
            }
        }
    }
}

@Composable
private fun TripSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .padding(top = 8.dp)
    )
}

@Composable
private fun TripCard(
    reservation: Reservation,
    onClick: () -> Unit
) {
    val phase = TripDomainRules.tripPhase(reservation)
    val phaseColor = if (phase == TripDomainRules.PHASE_IN_PROGRESS) {
        StatusApproved
    } else {
        StatusReview
    }

    Card(
        onClick = onClick,
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Explore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = reservation.destination,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = phaseColor.copy(alpha = 0.12f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = phase,
                            style = MaterialTheme.typography.labelSmall,
                            color = phaseColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = formatTripRange(reservation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${reservation.travelerCount} " +
                            if (reservation.travelerCount == 1) "viajero" else "viajeros",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Abrir viaje",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EmptyTripList(
    onViewReservations: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Explore,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(18.dp))
        Text("Aún no tienes viajes activos", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Cuando el pago de una reserva sea aprobado, podrás organizarla aquí.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onViewReservations) { Text("Revisar mis reservas") }
    }
}

@Composable
private fun TripListLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

private fun formatTripRange(reservation: Reservation): String {
    if (reservation.departureAtMillis <= 0L) return "Fecha por confirmar"
    val locale = Locale.forLanguageTag("es-PE")
    val start = SimpleDateFormat("d MMM yyyy", locale).format(Date(reservation.departureAtMillis))
    val end = reservation.returnAtMillis.takeIf { it > 0L }?.let {
        SimpleDateFormat("d MMM yyyy", locale).format(Date(it))
    }
    return if (end == null) start else "$start - $end"
}
