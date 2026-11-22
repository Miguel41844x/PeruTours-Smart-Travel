package com.example.perutours.ui.screens.admin.service

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.perutours.ui.components.TravelTopAppBar as TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.perutours.data.model.CatalogService
import com.example.perutours.data.model.TourDestination
import com.example.perutours.data.model.TourProvider
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminServicesScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminServicesViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Servicios del catálogo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.openEditor() }) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo servicio")
            }
        }
    ) { paddingValues ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.services.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Crea el primer servicio para construir paquetes y cotizaciones.")
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.services, key = CatalogService::id) { service ->
                    ServiceCard(
                        service = service,
                        onEdit = { viewModel.openEditor(service) },
                        onToggle = { viewModel.toggleActive(service) }
                    )
                }
            }
        }
    }

    if (state.isEditorOpen) {
        ServiceEditorDialog(
            service = state.editingService,
            destinations = state.destinations,
            providers = state.providers,
            isSaving = state.isSaving,
            onDismiss = viewModel::closeEditor,
            onSave = viewModel::saveService
        )
    }
}

@Composable
private fun ServiceCard(
    service: CatalogService,
    onEdit: () -> Unit,
    onToggle: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(service.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${service.city} · ${service.category}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    if (service.active) "Activo" else "Inactivo",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (service.active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                )
            }
            Text(
                "Proveedor: ${service.supplierName.ifBlank { "Sin proveedor" }}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Costo base: S/ ${"%.2f".format(service.unitCost)} · " +
                    if (service.requiresScheduling) "Requiere programación" else "Sin horario",
                style = MaterialTheme.typography.bodySmall
            )
            if (service.description.isNotBlank()) {
                Text(
                    service.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) { Text("Editar") }
                OutlinedButton(onClick = onToggle) {
                    Text(if (service.active) "Desactivar" else "Activar")
                }
            }
        }
    }
}

@Composable
private fun ServiceEditorDialog(
    service: CatalogService?,
    destinations: List<TourDestination>,
    providers: List<TourProvider>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, Boolean, Boolean, Boolean) -> Unit
) {
    var name by remember(service) { mutableStateOf(service?.name.orEmpty()) }
    var destinationId by remember(service) { mutableStateOf(service?.destinationId.orEmpty()) }
    var providerId by remember(service) { mutableStateOf(service?.providerId.orEmpty()) }
    var category by remember(service) { mutableStateOf(service?.category ?: CATEGORIES.first()) }
    var cost by remember(service) { mutableStateOf(service?.unitCost?.takeIf { it > 0 }?.toString().orEmpty()) }
    var description by remember(service) { mutableStateOf(service?.description.orEmpty()) }
    var duration by remember(service) { mutableStateOf((service?.durationMinutes ?: 60).toString()) }
    var requiresScheduling by remember(service) { mutableStateOf(service?.requiresScheduling ?: true) }
    var active by remember(service) { mutableStateOf(service?.active ?: true) }
    var requestable by remember(service) { mutableStateOf(service?.requestable ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (service == null) "Nuevo servicio" else "Editar servicio") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre *") },
                    modifier = Modifier.fillMaxWidth()
                )
                SelectionField(
                    label = "Destino *",
                    value = destinations.firstOrNull { it.id == destinationId }?.name.orEmpty(),
                    options = destinations.map { it.id to it.name },
                    onSelected = { destinationId = it }
                )
                SelectionField(
                    label = "Proveedor *",
                    value = providers.firstOrNull { it.id == providerId }?.name.orEmpty(),
                    options = providers.map { it.id to "${it.name}${if (it.active) "" else " (inactivo)"}" },
                    onSelected = { providerId = it }
                )
                SelectionField(
                    label = "Categoría *",
                    value = category,
                    options = CATEGORIES.map { it to it },
                    onSelected = { category = it }
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = { Text("Costo base (S/) *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter(Char::isDigit) },
                    label = { Text("Duración estimada (minutos) *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(1_000) },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                ToggleRow("Requiere fecha y hora", requiresScheduling) { requiresScheduling = it }
                ToggleRow("Servicio activo", active) { active = it }
                ToggleRow("Ofrecer como actividad opcional al viajero", requestable) {
                    requestable = it
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        destinationId,
                        providerId,
                        category,
                        cost,
                        description,
                        duration,
                        requiresScheduling,
                        active,
                        requestable
                    )
                },
                enabled = !isSaving
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun SelectionField(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(value.ifBlank { "Seleccionar" }, modifier = Modifier.weight(1f))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                options.forEach { (id, text) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            onSelected(id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private val CATEGORIES = listOf(
    "Alojamiento",
    "Transporte",
    "Tours",
    "Guía",
    "Alimentación",
    "Experiencias",
    "Aventura",
    "Seguro",
    "Otro"
)
