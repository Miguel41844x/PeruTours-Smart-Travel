package com.example.perutours.ui.screens.trip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.perutours.data.model.TripChecklistItem
import com.example.perutours.data.model.TripDomainRules
import com.example.perutours.data.model.TripOverview

internal fun androidx.compose.foundation.lazy.LazyListScope.tripChecklistItems(
    overview: TripOverview,
    isSaving: Boolean,
    itemInProgress: String?,
    onAdd: (String, String) -> Unit,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
    onSync: () -> Unit
) {
    item {
        TripChecklistSummary(
            overview = overview,
            isSaving = isSaving,
            onAdd = onAdd,
            onSync = onSync
        )
    }

    if (overview.checklist.isEmpty()) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(46.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Tu lista está vacía", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Agrega documentos, equipaje o cualquier pendiente antes de salir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        val grouped = overview.checklist.groupBy(TripChecklistItem::category)
        TripChecklistItem.VALID_CATEGORIES.forEach { category ->
            val items = grouped[category].orEmpty()
            if (items.isNotEmpty()) {
                item(key = "checklist-title-$category") {
                    Text(
                        category,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                            .padding(top = 6.dp)
                    )
                }
                items(items, key = TripChecklistItem::id) { checklistItem ->
                    TripChecklistRow(
                        item = checklistItem,
                        isBusy = itemInProgress == checklistItem.id,
                        onToggle = { onToggle(checklistItem.id) },
                        onDelete = { onDelete(checklistItem.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TripChecklistSummary(
    overview: TripOverview,
    isSaving: Boolean,
    onAdd: (String, String) -> Unit,
    onSync: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val total = overview.checklist.size
    val completed = overview.completedChecklistCount

    if (showDialog) {
        AddChecklistDialog(
            isSaving = isSaving,
            onDismiss = { if (!isSaving) showDialog = false },
            onConfirm = { title, category ->
                onAdd(title, category)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Preparación del viaje", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (total == 0) "Empieza con tu primer pendiente" else "$completed de $total completados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onSync) {
                    Icon(Icons.Default.CloudSync, contentDescription = "Sincronizar checklist")
                }
            }
            LinearProgressIndicator(
                progress = { overview.checklistProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
            )
            Button(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Agregar pendiente")
            }
        }
    }
}

@Composable
private fun TripChecklistRow(
    item: TripChecklistItem,
    isBusy: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
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
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isCompleted,
                onCheckedChange = { onToggle() },
                enabled = !isBusy
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.isCompleted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete, enabled = !isBusy) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar ${item.title}")
            }
        }
    }
}

@Composable
private fun AddChecklistDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TripChecklistItem.CATEGORY_LUGGAGE) }
    val validationError = TripDomainRules.validateChecklistTitle(title)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo pendiente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 100) title = it },
                    label = { Text("¿Qué necesitas recordar?") },
                    supportingText = { Text("${title.length}/100") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Categoría", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TripChecklistItem.VALID_CATEGORIES.forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text(option) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title.trim(), category) },
                enabled = validationError == null && !isSaving
            ) { Text("Agregar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancelar") }
        }
    )
}
