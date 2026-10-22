package com.example.perutours.ui.screens.trip

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.LocationOff
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.perutours.data.location.DetectedLocation
import com.example.perutours.data.model.TripDomainRules
import com.example.perutours.data.model.TripIncident
import com.example.perutours.data.model.TripOverview
import com.example.perutours.ui.theme.StatusApproved
import com.example.perutours.ui.theme.StatusCancelled
import com.example.perutours.ui.theme.StatusPending
import com.example.perutours.ui.theme.StatusReview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun androidx.compose.foundation.lazy.LazyListScope.tripSupportItems(
    overview: TripOverview,
    isSubmitting: Boolean,
    isDetectingLocation: Boolean,
    detectedLocation: DetectedLocation?,
    onDetectLocation: () -> Unit,
    onClearLocation: () -> Unit,
    onCreateIncident: (String, String, String) -> Unit
) {
    item {
        TripSupportSummary(
            openIncidentCount = overview.openIncidentCount,
            isSubmitting = isSubmitting,
            isDetectingLocation = isDetectingLocation,
            detectedLocation = detectedLocation,
            onDetectLocation = onDetectLocation,
            onClearLocation = onClearLocation,
            onCreateIncident = onCreateIncident
        )
    }

    if (overview.incidents.isEmpty()) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .padding(vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.SupportAgent,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Todo marcha bien", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Si surge un problema relacionado con el viaje, comunícalo desde aquí.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        item {
            Text(
                "Historial de asistencia",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .padding(top = 6.dp)
            )
        }
        items(overview.incidents, key = TripIncident::id) { incident ->
            TripIncidentCard(incident)
        }
    }
}

@Composable
private fun TripSupportSummary(
    openIncidentCount: Int,
    isSubmitting: Boolean,
    isDetectingLocation: Boolean,
    detectedLocation: DetectedLocation?,
    onDetectLocation: () -> Unit,
    onClearLocation: () -> Unit,
    onCreateIncident: (String, String, String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        CreateIncidentDialog(
            isSubmitting = isSubmitting,
            isDetectingLocation = isDetectingLocation,
            detectedLocation = detectedLocation,
            onDetectLocation = onDetectLocation,
            onClearLocation = onClearLocation,
            onDismiss = {
                if (!isSubmitting) {
                    showDialog = false
                    onClearLocation()
                }
            },
            onConfirm = { description, category, priority ->
                onCreateIncident(description, category, priority)
                showDialog = false
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SupportAgent,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Asistencia durante el viaje", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (openIncidentCount == 0) {
                            "No tienes solicitudes abiertas"
                        } else {
                            "$openIncidentCount ${if (openIncidentCount == 1) "solicitud abierta" else "solicitudes abiertas"}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "Este canal atiende inconvenientes de tus servicios contratados. No reemplaza a los servicios locales de emergencia.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AddAlert, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Reportar un problema")
            }
        }
    }
}

@Composable
private fun CreateIncidentDialog(
    isSubmitting: Boolean,
    isDetectingLocation: Boolean,
    detectedLocation: DetectedLocation?,
    onDetectLocation: () -> Unit,
    onClearLocation: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TripIncident.CATEGORY_TRANSPORT) }
    var priority by remember { mutableStateOf(TripIncident.PRIORITY_MEDIUM) }
    val validationError = TripDomainRules.validateIncident(description, category, priority)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) onDetectLocation()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reportar un problema") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 1_000) description = it },
                    label = { Text("Describe lo ocurrido") },
                    supportingText = { Text("${description.length}/1000") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Categoría", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TripIncident.VALID_CATEGORIES.forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text(option) }
                        )
                    }
                }
                Text("Prioridad", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TripIncident.VALID_PRIORITIES.forEach { option ->
                        FilterChip(
                            selected = priority == option,
                            onClick = { priority = option },
                            label = { Text(option) }
                        )
                    }
                }
                if (detectedLocation == null) {
                    OutlinedButton(
                        onClick = {
                            val fineGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val coarseGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (fineGranted || coarseGranted) {
                                onDetectLocation()
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        enabled = !isDetectingLocation,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isDetectingLocation) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Adjuntar ubicación actual")
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                detectedLocation.city.ifBlank { "Ubicación obtenida" },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onClearLocation) {
                                Icon(Icons.Default.LocationOff, contentDescription = "Quitar ubicación")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(description.trim(), category, priority) },
                enabled = validationError == null && !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Enviar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Cancelar") }
        }
    )
}

@Composable
private fun TripIncidentCard(incident: TripIncident) {
    val statusColor = when (incident.status) {
        TripIncident.STATUS_RESOLVED -> StatusApproved
        TripIncident.STATUS_CLOSED -> MaterialTheme.colorScheme.onSurfaceVariant
        TripIncident.STATUS_IN_REVIEW -> StatusReview
        else -> StatusPending
    }
    val priorityColor = when (incident.priority) {
        TripIncident.PRIORITY_URGENT, TripIncident.PRIORITY_HIGH -> StatusCancelled
        TripIncident.PRIORITY_MEDIUM -> StatusPending
        else -> StatusApproved
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
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
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
            Text(incident.description, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = priorityColor.copy(alpha = 0.12f), shape = CircleShape) {
                    Text(
                        "Prioridad ${incident.priority.lowercase()}",
                        color = priorityColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    formatIncidentDate(incident.createdAtMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (incident.locationName.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(incident.locationName, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (incident.agentResponse.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text("Respuesta del agente", style = MaterialTheme.typography.labelLarge)
                        Text(incident.agentResponse, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private fun formatIncidentDate(value: Long): String = if (value <= 0L) {
    "Fecha no disponible"
} else {
    SimpleDateFormat(
        "d MMM, HH:mm",
        Locale.forLanguageTag("es-PE")
    ).format(Date(value))
}
