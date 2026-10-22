package com.example.perutours.ui.screens.admin.destination

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel

/**
 * Formulario de destino del catálogo turístico.
 *
 * El administrador registra el lugar, guarda sus coordenadas y decide si lo
 * deja como borrador o lo publica para los clientes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDestinationFormScreen(
    destinationId: String,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AdminDestinationFormViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let(viewModel::onCoverImagePicked)
    }

    LaunchedEffect(destinationId) { viewModel.load(destinationId) }

    LaunchedEffect(uiState.savedDestinationId) {
        if (uiState.savedDestinationId != null) onSaved()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isEditing) "Editar destino" else "Nuevo destino")
                },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nombre del destino *") },
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.department,
                onValueChange = viewModel::onDepartmentChange,
                label = { Text("Departamento o región *") },
                isError = uiState.departmentError != null,
                supportingText = uiState.departmentError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.summary,
                onValueChange = viewModel::onSummaryChange,
                label = { Text("Resumen del destino") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.latitudeText,
                    onValueChange = viewModel::onLatitudeChange,
                    label = { Text("Latitud") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = uiState.longitudeText,
                    onValueChange = viewModel::onLongitudeChange,
                    label = { Text("Longitud") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            if (uiState.coordinatesError != null) {
                Text(
                    text = uiState.coordinatesError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedTextField(
                value = uiState.coverImageUrl,
                onValueChange = viewModel::onImageUrlChange,
                label = { Text("URL de la imagen de portada") },
                isError = uiState.imageUrlError != null,
                supportingText = uiState.imageUrlError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // HU catálogo: la imagen se sube a Firebase Storage y su URL
            // pública queda guardada en el documento del destino.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { imagePicker.launch("image/*") }, enabled = !uiState.isUploadingImage) {
                    Text("Subir imagen")
                }
                if (uiState.isUploadingImage) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }

            if (uiState.coverImageUrl.isNotBlank()) {
                AsyncImage(
                    model = uiState.coverImageUrl,
                    contentDescription = "Imagen de portada del destino",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            }

            OutlinedTextField(
                value = uiState.categoriesText,
                onValueChange = viewModel::onCategoriesChange,
                label = { Text("Categorías (separadas por coma)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.externalPlaceId,
                onValueChange = viewModel::onExternalPlaceIdChange,
                label = { Text("ID externo del lugar (Geoapify)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { viewModel.save(shouldPublish = false) },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Guardar borrador")
                }
                Button(
                    onClick = { viewModel.save(shouldPublish = true) },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Publicar")
                }
            }
        }
    }
}
