package com.example.perutours.ui.screens.profile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import coil.compose.AsyncImage
import com.example.perutours.ui.components.PeruToursTopBar
import com.example.perutours.ui.components.ScreenHorizontalPadding
import com.example.perutours.ui.components.SectionHeading
import kotlinx.coroutines.launch
import java.io.File

private data class ProfileSignature(
    val name: String,
    val phone: String,
    val dni: String,
    val city: String,
    val preferences: Set<String>
)

private fun ProfileUiState.signature() = ProfileSignature(
    name = name,
    phone = phone,
    dni = dni,
    city = city,
    preferences = selectedPreferences
)

private fun createTempImageUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val imageFile = File.createTempFile("perfil_${System.currentTimeMillis()}_", ".jpg", imagesDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    var showPhotoSourceSheet by remember { mutableStateOf(false) }
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var baseline by remember { mutableStateOf<ProfileSignature?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val hasUnsavedChanges = baseline?.let { it != uiState.signature() } == true

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(viewModel::uploadPhoto) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val capturedUri = pendingCameraUri?.let(Uri::parse)
        pendingCameraUri = null
        if (success && capturedUri != null) viewModel.uploadPhoto(capturedUri)
    }

    fun openCamera() {
        val newUri = createTempImageUri(context)
        pendingCameraUri = newUri.toString()
        cameraLauncher.launch(newUri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Permite el acceso a la cámara para tomar una foto.")
            }
        }
    }

    fun requestCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) openCamera()
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun requestBack() {
        if (hasUnsavedChanges) showDiscardDialog = true else onBack()
    }

    BackHandler(enabled = hasUnsavedChanges) { showDiscardDialog = true }

    LaunchedEffect(uiState.isLoadingInitialData) {
        if (!uiState.isLoadingInitialData && baseline == null) baseline = uiState.signature()
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            if (message.contains("guardados con éxito", ignoreCase = true)) {
                baseline = uiState.signature()
            }
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("¿Salir sin guardar?") },
            text = { Text("Los cambios realizados en tu perfil se perderán.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBack()
                    }
                ) { Text("Salir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Seguir editando") }
            }
        )
    }

    if (showPhotoSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoSourceSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Actualizar foto",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                ListItem(
                    modifier = Modifier.clickable {
                        showPhotoSourceSheet = false
                        requestCamera()
                    },
                    headlineContent = { Text("Tomar una foto") },
                    supportingContent = { Text("Usar la cámara del teléfono") },
                    leadingContent = { Icon(Icons.Default.PhotoCamera, contentDescription = null) }
                )
                ListItem(
                    modifier = Modifier.clickable {
                        showPhotoSourceSheet = false
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    headlineContent = { Text("Elegir de la galería") },
                    supportingContent = { Text("Seleccionar una imagen guardada") },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) }
                )
            }
        }
    }

    Scaffold(
        topBar = { PeruToursTopBar(title = "Mi perfil", onBack = ::requestBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!uiState.isLoadingInitialData) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .padding(horizontal = ScreenHorizontalPadding, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.saveProfile()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 720.dp)
                                .height(52.dp),
                            enabled = hasUnsavedChanges && !uiState.isSaving && !uiState.isUploadingPhoto
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text("Guardando...", modifier = Modifier.padding(start = 10.dp))
                            } else {
                                Text(if (hasUnsavedChanges) "Guardar cambios" else "Perfil actualizado")
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoadingInitialData) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = ScreenHorizontalPadding, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    ProfilePhoto(
                        state = uiState,
                        onEditPhoto = { showPhotoSourceSheet = true },
                        onRetryPhoto = viewModel::retryPhotoUpload
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    SectionHeading(
                        title = "Datos personales",
                        supportingText = "Usaremos esta información para tus solicitudes y reservas."
                    )

                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = viewModel::onNameChanged,
                        label = { Text("Nombre completo") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        isError = uiState.nameError != null,
                        supportingText = uiState.nameError?.let { error -> { Text(error) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        value = uiState.phone,
                        onValueChange = viewModel::onPhoneChanged,
                        label = { Text("Celular") },
                        prefix = { Text("+51 ") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        isError = uiState.phoneError != null,
                        supportingText = uiState.phoneError?.let { error -> { Text(error) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        value = uiState.city,
                        onValueChange = viewModel::onCityChanged,
                        label = { Text("Ciudad y país") },
                        placeholder = { Text("Ej. Lima, Perú") },
                        leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                        isError = uiState.cityError != null,
                        supportingText = uiState.cityError?.let { error -> { Text(error) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        value = uiState.dni,
                        onValueChange = viewModel::onDniChanged,
                        label = { Text("DNI o pasaporte (opcional)") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        isError = uiState.dniError != null,
                        supportingText = uiState.dniError?.let { error -> { Text(error) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Correo verificado", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    uiState.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    SectionHeading(
                        title = "Preferencias de viaje",
                        supportingText = "Elige al menos una para personalizar las recomendaciones."
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.availablePreferences.forEach { preference ->
                            val isSelected = preference.id in uiState.selectedPreferences
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.togglePreference(preference.id) },
                                label = { Text(preference.name) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }

                    uiState.preferencesError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePhoto(
    state: ProfileUiState,
    onEditPhoto: () -> Unit,
    onRetryPhoto: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            val imageModel: Any? = state.localPhotoUri ?: state.photoUrl.takeIf(String::isNotBlank)
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable(onClick = onEditPhoto),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier
                        .size(104.dp)
                        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable(onClick = onEditPhoto),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = state.name.firstOrNull()?.uppercase() ?: "P",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            SmallFloatingActionButton(onClick = onEditPhoto) {
                if (state.isUploadingPhoto) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Cambiar foto")
                }
            }
        }

        if (state.photoUploadState is PhotoUploadState.Error) {
            TextButton(onClick = onRetryPhoto) {
                Text("Reintentar subir foto", color = MaterialTheme.colorScheme.error)
            }
        }

        Text(
            text = state.name.ifBlank { "Tu perfil" },
            style = MaterialTheme.typography.titleLarge
        )
        TextButton(onClick = onEditPhoto) { Text("Cambiar foto") }
    }
}
