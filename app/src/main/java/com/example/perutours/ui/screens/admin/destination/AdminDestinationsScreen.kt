package com.example.perutours.ui.screens.admin.destination

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.perutours.data.model.CatalogStatus
import com.example.perutours.data.model.TourDestination
import org.koin.androidx.compose.koinViewModel

/**
 * Panel del administrador para gestionar los destinos del catálogo.
 * Permite crear, editar y mover cada destino por su ciclo de vida:
 * BORRADOR -> PUBLICADO -> ARCHIVADO.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDestinationsScreen(
    onNavigateBack: () -> Unit,
    onCreateDestination: () -> Unit,
    onEditDestination: (String) -> Unit,
    viewModel: AdminDestinationsViewModel = koinViewModel()
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
                title = { Text("Destinos del catálogo") },
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
            FloatingActionButton(onClick = onCreateDestination) {
                Icon(Icons.Default.Add, contentDescription = "Registrar destino")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading -> LoadingState(paddingValues)
            uiState.errorMessage != null && uiState.destinations.isEmpty() ->
                Text(
                    text = uiState.errorMessage.orEmpty(),
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(16.dp)
                )
            uiState.destinations.isEmpty() -> Text(
                text = "Aún no registras destinos. Toca + para crear el primero.",
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
                items(uiState.destinations, key = { it.id }) { destination ->
                    DestinationAdminCard(
                        destination = destination,
                        onEdit = { onEditDestination(destination.id) },
                        onPublish = {
                            viewModel.changeStatus(destination, CatalogStatus.PUBLICADO)
                        },
                        onArchive = {
                            viewModel.changeStatus(destination, CatalogStatus.ARCHIVADO)
                        },
                        onReturnToDraft = {
                            viewModel.changeStatus(destination, CatalogStatus.BORRADOR)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text("Cargando destinos...")
    }
}

@Composable
private fun DestinationAdminCard(
    destination: TourDestination,
    onEdit: () -> Unit,
    onPublish: () -> Unit,
    onArchive: () -> Unit,
    onReturnToDraft: () -> Unit
) {
    androidx.compose.material3.Card(
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
                    text = destination.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                StatusTag(status = CatalogStatus.from(destination.status))
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = destination.department,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (destination.hasCoordinates) {
                Text(
                    text = "Coordenadas: ${destination.latitude}, ${destination.longitude}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Sin coordenadas validadas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) { Text("Editar") }
                when (CatalogStatus.from(destination.status)) {
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
fun StatusTag(status: CatalogStatus) {
    val color = when (status) {
        CatalogStatus.PUBLICADO -> MaterialTheme.colorScheme.primary
        CatalogStatus.BORRADOR -> MaterialTheme.colorScheme.tertiary
        CatalogStatus.ARCHIVADO -> MaterialTheme.colorScheme.outline
    }
    AssistChip(
        onClick = {},
        label = { Text(status.name.lowercase().replaceFirstChar { it.uppercase() }) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.14f),
            labelColor = color
        )
    )
}
