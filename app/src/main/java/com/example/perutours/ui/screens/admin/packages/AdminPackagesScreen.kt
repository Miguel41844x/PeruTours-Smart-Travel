package com.example.perutours.ui.screens.admin.packages

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.example.perutours.data.model.CatalogStatus
import com.example.perutours.data.model.TourPackage
import com.example.perutours.ui.screens.admin.destination.StatusTag
import org.koin.androidx.compose.koinViewModel

/**
 * Panel del administrador para paquetes turísticos: construcción de la
 * oferta, servicios incluidos y publicación del catálogo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPackagesScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminPackagesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.feedbackMessage, uiState.errorMessage) {
        val message = uiState.feedbackMessage ?: uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paquetes turísticos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.openEditor() }) {
                Icon(Icons.Default.Add, contentDescription = "Crear paquete")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }

            uiState.packages.isEmpty() -> Text(
                text = "Aún no hay paquetes. Toca + para crear el primero.",
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.packages, key = { it.id }) { tourPackage ->
                    PackageCard(
                        tourPackage = tourPackage,
                        destinationName = viewModel.destinationNameOf(tourPackage.destinationId),
                        onEdit = { viewModel.openEditor(tourPackage) },
                        onPublish = {
                            viewModel.changeStatus(tourPackage, CatalogStatus.PUBLICADO)
                        },
                        onArchive = {
                            viewModel.changeStatus(tourPackage, CatalogStatus.ARCHIVADO)
                        },
                        onReturnToDraft = {
                            viewModel.changeStatus(tourPackage, CatalogStatus.BORRADOR)
                        }
                    )
                }
            }
        }
    }

    if (uiState.isEditorOpen) {
        PackageEditorDialog(
            tourPackage = uiState.editingPackage,
            destinations = uiState.destinations,
            catalogServices = uiState.catalogServices,
            isSaving = uiState.isSaving,
            onDismiss = viewModel::closeEditor,
            onSave = viewModel::savePackage
        )
    }
}

@Composable
private fun PackageCard(
    tourPackage: TourPackage,
    destinationName: String,
    onEdit: () -> Unit,
    onPublish: () -> Unit,
    onArchive: () -> Unit,
    onReturnToDraft: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tourPackage.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                StatusTag(status = CatalogStatus.from(tourPackage.status))
            }

            Text(
                text = destinationName.ifBlank { "Sin destino" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${tourPackage.priceLabel()} · ${tourPackage.durationLabel()}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (tourPackage.includedServiceIds.isNotEmpty()) {
                Text(
                    text = "${tourPackage.includedServiceIds.size} servicios incluidos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) { Text("Editar") }
                when (CatalogStatus.from(tourPackage.status)) {
                    CatalogStatus.PUBLICADO -> OutlinedButton(onClick = onArchive) {
                        Text("Archivar")
                    }
                    CatalogStatus.ARCHIVADO -> Button(onClick = onReturnToDraft) {
                        Text("Reabrir")
                    }
                    CatalogStatus.BORRADOR -> Button(onClick = onPublish) {
                        Text("Publicar")
                    }
                }
            }
        }
    }
}

@Composable
private fun PackageEditorDialog(
    tourPackage: TourPackage?,
    destinations: List<com.example.perutours.data.model.TourDestination>,
    catalogServices: List<com.example.perutours.data.model.CatalogService>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (TourPackage) -> Unit
) {
    var destinationId by remember(tourPackage) {
        mutableStateOf(tourPackage?.destinationId.orEmpty())
    }
    var title by remember(tourPackage) { mutableStateOf(tourPackage?.title.orEmpty()) }
    var summary by remember(tourPackage) { mutableStateOf(tourPackage?.summary.orEmpty()) }
    var daysText by remember(tourPackage) {
        mutableStateOf((tourPackage?.durationDays ?: 1).toString())
    }
    var nightsText by remember(tourPackage) {
        mutableStateOf(((tourPackage?.durationDays ?: 1) - 1).coerceAtLeast(0).toString())
    }
    var priceText by remember(tourPackage) {
        mutableStateOf(tourPackage?.priceAmount?.takeIf { it > 0.0 }?.let { amount ->
            "%,.0f".format(amount).replace(',', ' ')
        }.orEmpty())
    }
    var isUsd by remember(tourPackage) { mutableStateOf(tourPackage?.currency == "USD") }
    var ratingText by remember(tourPackage) {
        mutableStateOf((tourPackage?.ratingScore ?: 4.5).toString())
    }
    var category by remember(tourPackage) { mutableStateOf(tourPackage?.category ?: "Popular") }
    var coverImageUrl by remember(tourPackage) { mutableStateOf(tourPackage?.coverImageUrl.orEmpty()) }
    var conditions by remember(tourPackage) { mutableStateOf(tourPackage?.conditions.orEmpty()) }
    var includedServiceIds by remember(tourPackage) {
        mutableStateOf(tourPackage?.includedServiceIds.orEmpty())
    }
    var destinationMenuExpanded by remember { mutableStateOf(false) }
    var isServicesDialogOpen by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    var destinationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tourPackage == null) "Nuevo paquete" else "Editar paquete") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box {
                    OutlinedButton(
                        onClick = { destinationMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = destinations.firstOrNull { it.id == destinationId }?.name
                                ?: "Seleccionar destino *",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    DropdownMenu(
                        expanded = destinationMenuExpanded,
                        onDismissRequest = { destinationMenuExpanded = false }
                    ) {
                        destinations.forEach { destination ->
                            DropdownMenuItem(
                                text = { Text(destination.name) },
                                onClick = {
                                    destinationId = destination.id
                                    destinationError = null
                                    destinationMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                destinationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = null
                    },
                    label = { Text("Título del paquete *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                titleError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Descripción de la oferta") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = daysText,
                        onValueChange = { daysText = it.filter(Char::isDigit).take(3) },
                        label = { Text("Días") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = nightsText,
                        onValueChange = { nightsText = it.filter(Char::isDigit).take(3) },
                        label = { Text("Noches") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = {
                            priceText = it
                            priceError = null
                        },
                        label = { Text("Precio *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { isUsd = !isUsd },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text(if (isUsd) "USD" else "PEN")
                    }
                }
                priceError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                OutlinedTextField(
                    value = ratingText,
                    onValueChange = { ratingText = it.filter { char -> char.isDigit() || char == '.' }.take(4) },
                    label = { Text("Calificación (0 - 5)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoría") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = coverImageUrl,
                    onValueChange = { coverImageUrl = it },
                    label = { Text("URL de la imagen") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = conditions,
                    onValueChange = { conditions = it },
                    label = { Text("Condiciones comerciales") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { isServicesDialogOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (includedServiceIds.isEmpty()) {
                            "Seleccionar servicios incluidos"
                        } else {
                            "${includedServiceIds.size} servicios incluidos"
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    val days = daysText.toIntOrNull()?.coerceIn(1, 365) ?: 1
                    val nights = nightsText.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    titleError = PackageFormValidator.validateTitle(title)
                    priceError = PackageFormValidator.validatePrice(priceText)
                    destinationError = PackageFormValidator.validateDestination(destinationId)

                    if (titleError != null || priceError != null || destinationError != null) {
                        return@Button
                    }

                    onSave(
                        PackageFormValidator.buildPackage(
                            existing = tourPackage,
                            destinationId = destinationId,
                            title = title,
                            summary = summary,
                            durationText = PackageFormValidator.buildDurationText(days, nights),
                            durationDays = days,
                            priceText = priceText,
                            currency = if (isUsd) "USD" else "PEN",
                            ratingScore = ratingText.toDoubleOrNull() ?: 4.5,
                            category = category,
                            coverImageUrl = coverImageUrl,
                            conditions = conditions,
                            includedServiceIds = includedServiceIds,
                            status = CatalogStatus.from(tourPackage?.status),
                            createdBy = tourPackage?.createdBy.orEmpty(),
                            nowMillis = System.currentTimeMillis()
                        )
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )

    if (isServicesDialogOpen) {
        ServicesPickerDialog(
            catalogServices = catalogServices,
            selectedIds = includedServiceIds,
            onDismiss = { isServicesDialogOpen = false },
            onConfirm = { selected ->
                includedServiceIds = selected
                isServicesDialogOpen = false
            }
        )
    }
}

@Composable
private fun ServicesPickerDialog(
    catalogServices: List<com.example.perutours.data.model.CatalogService>,
    selectedIds: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var selection by remember(selectedIds) { mutableStateOf(selectedIds.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Servicios incluidos") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(catalogServices, key = { it.id }) { service ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = selection.contains(service.id),
                            onCheckedChange = { checked ->
                                selection = if (checked) selection + service.id
                                else selection - service.id
                            }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(service.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${service.city} · S/ ${service.unitCost.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selection.toList().take(10)) }) { Text("Listo") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
