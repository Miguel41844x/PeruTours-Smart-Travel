package com.example.perutours.ui.screens.profile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.perutours.ui.theme.BackgroundLight
import com.example.perutours.ui.theme.PeruGold40
import com.example.perutours.ui.theme.SurfaceLight
import kotlinx.coroutines.launch
import java.io.File

private fun createTempImageUri(
    context: Context
): Uri {

    val imagesDir =
        File(context.cacheDir, "images")
            .apply { mkdirs() }

    val imageFile =
        File.createTempFile(
            "perfil_${System.currentTimeMillis()}_",
            ".jpg",
            imagesDir
        )

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {

    val context = LocalContext.current

    val uiState by viewModel.uiState
        .collectAsStateWithLifecycle()

    val coroutineScope =
        rememberCoroutineScope()

    val snackbarHostState =
        remember {
            SnackbarHostState()
        }

    var showPhotoSourceSheet by remember {
        mutableStateOf(false)
    }

    var pendingCameraUri by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    // ============================================================
    // GALERÍA
    // ============================================================

    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->

            if (uri != null) {
                viewModel.uploadPhoto(uri)
            }
        }

    // ============================================================
    // CÁMARA
    // ============================================================

    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.TakePicture()
        ) { success: Boolean ->

            val capturedUri =
                pendingCameraUri?.let(Uri::parse)

            pendingCameraUri = null

            if (
                success &&
                capturedUri != null
            ) {

                viewModel.uploadPhoto(
                    capturedUri
                )
            }
        }

    // ============================================================
    // PERMISO CÁMARA
    // ============================================================

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->

            if (isGranted) {

                val newUri =
                    createTempImageUri(
                        context
                    )

                pendingCameraUri =
                    newUri.toString()

                cameraLauncher.launch(
                    newUri
                )

            } else {

                coroutineScope.launch {

                    snackbarHostState.showSnackbar(
                        "Permiso de cámara denegado. Habilítalo para tomar tu foto."
                    )
                }
            }
        }

    // ============================================================
    // MENSAJES
    // ============================================================

    LaunchedEffect(uiState.message) {

        uiState.message?.let { message ->

            snackbarHostState.showSnackbar(
                message
            )

            viewModel.clearMessage()
        }
    }

    // ============================================================
    // BOTTOM SHEET
    // ============================================================

    if (showPhotoSourceSheet) {

        ModalBottomSheet(
            onDismissRequest = {
                showPhotoSourceSheet = false
            },
            containerColor = SurfaceLight
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 24.dp,
                            vertical = 16.dp
                        )
            ) {

                Text(
                    text =
                        "Actualizar foto de perfil",
                    fontSize = 18.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "Selecciona cómo deseas subir tu fotografía a PeruTours",
                    fontSize = 13.sp,
                    color =
                        Color(0xFF78716C),
                    modifier =
                        Modifier.padding(
                            top = 4.dp,
                            bottom = 16.dp
                        )
                )

                // =================================================
                // CÁMARA
                // =================================================

                OutlinedCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {

                                showPhotoSourceSheet =
                                    false

                                val hasPermission =
                                    ContextCompat
                                        .checkSelfPermission(
                                            context,
                                            Manifest.permission.CAMERA
                                        ) ==
                                            PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {

                                    val newUri =
                                        createTempImageUri(
                                            context
                                        )

                                    pendingCameraUri =
                                        newUri.toString()

                                    cameraLauncher.launch(
                                        newUri
                                    )

                                } else {

                                    cameraPermissionLauncher
                                        .launch(
                                            Manifest.permission.CAMERA
                                        )
                                }
                            },
                    shape =
                        RoundedCornerShape(14.dp)
                ) {

                    Row(
                        modifier =
                            Modifier.padding(16.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.PhotoCamera,
                            contentDescription =
                                null,
                            tint =
                                PeruGold40
                        )

                        Spacer(
                            modifier =
                                Modifier.width(14.dp)
                        )

                        Column {

                            Text(
                                "Tomar foto con la Cámara",
                                fontWeight =
                                    FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            Text(
                                "Captura una nueva foto en este momento",
                                fontSize = 12.sp,
                                color =
                                    Color(0xFF78716C)
                            )
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                // =================================================
                // GALERÍA
                // =================================================

                OutlinedCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {

                                showPhotoSourceSheet =
                                    false

                                galleryLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts
                                            .PickVisualMedia
                                            .ImageOnly
                                    )
                                )
                            },
                    shape =
                        RoundedCornerShape(14.dp)
                ) {

                    Row(
                        modifier =
                            Modifier.padding(16.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.PhotoLibrary,
                            contentDescription =
                                null,
                            tint =
                                PeruGold40
                        )

                        Spacer(
                            modifier =
                                Modifier.width(14.dp)
                        )

                        Column {

                            Text(
                                "Seleccionar desde Galería",
                                fontWeight =
                                    FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            Text(
                                "Elige una imagen guardada en tu teléfono",
                                fontSize = 12.sp,
                                color =
                                    Color(0xFF78716C)
                            )
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )
            }
        }
    }

    // ============================================================
    // PANTALLA PRINCIPAL
    // ============================================================

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text =
                            "Mi Perfil y Preferencias",
                        fontWeight =
                            FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription =
                                "Volver"
                        )
                    }
                },

                colors =
                    TopAppBarDefaults
                        .topAppBarColors(
                            containerColor =
                                SurfaceLight
                        )
            )
        },

        snackbarHost = {

            SnackbarHost(
                hostState =
                    snackbarHostState
            )
        },

        containerColor =
            BackgroundLight

    ) { paddingValues ->

        if (uiState.isLoadingInitialData) {

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            paddingValues
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                CircularProgressIndicator(
                    color =
                        PeruGold40
                )
            }

        } else {

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            paddingValues
                        )
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(20.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                // =================================================
                // FOTO DE PERFIL
                // =================================================

                Box(
                    contentAlignment =
                        Alignment.BottomEnd,

                    modifier =
                        Modifier.padding(
                            vertical = 8.dp
                        )
                ) {

                    val imageModel: Any? =
                        uiState.pendingPhotoUri
                            ?: uiState.photoUrl
                                .takeIf {
                                    it.isNotBlank()
                                }

                    if (imageModel != null) {

                        AsyncImage(
                            model =
                                imageModel,

                            contentDescription =
                                "Foto de perfil",

                            modifier =
                                Modifier
                                    .size(112.dp)
                                    .clip(
                                        CircleShape
                                    )
                                    .border(
                                        3.dp,
                                        PeruGold40,
                                        CircleShape
                                    )
                                    .clickable {
                                        showPhotoSourceSheet =
                                            true
                                    },

                            contentScale =
                                ContentScale.Crop
                        )

                    } else {

                        Surface(

                            modifier =
                                Modifier
                                    .size(112.dp)
                                    .clip(
                                        CircleShape
                                    )
                                    .border(
                                        3.dp,
                                        PeruGold40,
                                        CircleShape
                                    )
                                    .clickable {
                                        showPhotoSourceSheet =
                                            true
                                    },

                            color =
                                PeruGold40.copy(
                                    alpha = 0.15f
                                )

                        ) {

                            Box(
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Text(
                                    text =
                                        uiState.name
                                            .firstOrNull()
                                            ?.uppercase()
                                            ?: "P",

                                    fontSize =
                                        40.sp,

                                    fontWeight =
                                        FontWeight.ExtraBold,

                                    color =
                                        PeruGold40
                                )
                            }
                        }
                    }

                    SmallFloatingActionButton(

                        onClick = {
                            showPhotoSourceSheet =
                                true
                        },

                        containerColor =
                            PeruGold40,

                        contentColor =
                            Color.White,

                        shape =
                            CircleShape,

                        modifier =
                            Modifier.size(38.dp)

                    ) {

                        if (
                            uiState.isUploadingPhoto
                        ) {

                            CircularProgressIndicator(
                                color =
                                    Color.White,
                                strokeWidth =
                                    2.dp,
                                modifier =
                                    Modifier.size(
                                        18.dp
                                    )
                            )

                        } else {

                            Icon(
                                imageVector =
                                    Icons.Default.CameraAlt,

                                contentDescription =
                                    "Cambiar foto",

                                modifier =
                                    Modifier.size(
                                        18.dp
                                    )
                            )
                        }
                    }
                }

                TextButton(
                    onClick = {
                        showPhotoSourceSheet =
                            true
                    }
                ) {

                    Text(
                        text =
                            if (
                                uiState.isUploadingPhoto
                            ) {
                                "Subiendo foto a Firebase Storage..."
                            } else {
                                "Cambiar foto (Cámara o Galería)"
                            },

                        color =
                            PeruGold40,

                        fontWeight =
                            FontWeight.Bold,

                        fontSize = 13.sp
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                // =================================================
                // INFORMACIÓN PERSONAL
                // =================================================

                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(18.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                SurfaceLight
                        ),

                    border =
                        BorderStroke(
                            1.dp,
                            Color(0xFFE7E5E4)
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(18.dp)
                    ) {

                        Text(
                            text =
                                "Información Personal",

                            fontSize =
                                16.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface
                        )

                        Text(
                            text =
                                "Los campos marcados con (*) son obligatorios.",

                            fontSize =
                                12.sp,

                            color =
                                Color(0xFF78716C),

                            modifier =
                                Modifier.padding(
                                    top = 2.dp,
                                    bottom = 14.dp
                                )
                        )

                        // NOMBRE

                        OutlinedTextField(

                            value =
                                uiState.name,

                            onValueChange =
                                viewModel::onNameChanged,

                            label = {
                                Text(
                                    "Nombre completo *"
                                )
                            },

                            leadingIcon = {

                                Icon(
                                    Icons.Default.Person,
                                    contentDescription =
                                        null,
                                    tint =
                                        PeruGold40
                                )
                            },

                            isError =
                                uiState.nameError != null,

                            supportingText = {

                                uiState.nameError?.let {

                                    Text(
                                        text = it,
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .error
                                    )
                                }
                            },

                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(14.dp),

                            singleLine = true
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        // TELÉFONO

                        OutlinedTextField(

                            value =
                                uiState.phone,

                            onValueChange =
                                viewModel::onPhoneChanged,

                            label = {
                                Text(
                                    "Teléfono celular (9 dígitos) *"
                                )
                            },

                            leadingIcon = {

                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription =
                                        null,
                                    tint =
                                        PeruGold40
                                )
                            },

                            prefix = {
                                Text("+51 ")
                            },

                            isError =
                                uiState.phoneError != null,

                            supportingText = {

                                uiState.phoneError?.let {

                                    Text(
                                        text = it,
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .error
                                    )
                                }
                            },

                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(14.dp),

                            singleLine = true,

                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType =
                                        KeyboardType.Number
                                )
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        // CIUDAD

                        OutlinedTextField(

                            value =
                                uiState.city,

                            onValueChange =
                                viewModel::onCityChanged,

                            label = {
                                Text(
                                    "Ciudad / País de origen *"
                                )
                            },

                            leadingIcon = {

                                Icon(
                                    Icons.Default.LocationCity,
                                    contentDescription =
                                        null,
                                    tint =
                                        PeruGold40
                                )
                            },

                            isError =
                                uiState.cityError != null,

                            supportingText = {

                                uiState.cityError?.let {

                                    Text(
                                        text = it,
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .error
                                    )
                                }
                            },

                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(14.dp),

                            singleLine = true
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        // DNI

                        OutlinedTextField(
                            value = uiState.dni,
                            onValueChange = viewModel::onDniChanged,
                            label = { Text("DNI / Pasaporte (Opcional)") },
                            isError = uiState.dniError != null,
                            supportingText = {
                                uiState.dniError?.let {
                                    Text(
                                        text = it,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },

                            leadingIcon = {

                                Icon(
                                    Icons.Default.Badge,
                                    contentDescription =
                                        null,
                                    tint =
                                        PeruGold40
                                )
                            },

                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(14.dp),

                            singleLine = true
                        )

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )

                        // CORREO

                        OutlinedTextField(

                            value =
                                uiState.email,

                            onValueChange = {},

                            enabled = false,

                            label = {
                                Text(
                                    "Correo verificado"
                                )
                            },

                            leadingIcon = {

                                Icon(
                                    Icons.Default.Email,
                                    contentDescription =
                                        null,
                                    tint =
                                        Color(0xFF78716C)
                                )
                            },

                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(14.dp),

                            singleLine = true
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )

                // =================================================
                // PREFERENCIAS
                // =================================================

                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(18.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                SurfaceLight
                        ),

                    border =
                        BorderStroke(
                            1.dp,

                            if (
                                uiState.preferencesError != null
                            ) {
                                MaterialTheme
                                    .colorScheme
                                    .error
                            } else {
                                Color(0xFFE7E5E4)
                            }
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(18.dp)
                    ) {

                        Text(
                            text =
                                "Mis Preferencias de Viaje *",

                            fontSize =
                                16.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface
                        )

                        Text(
                            text =
                                "Elige al menos 1 interés para recibir recomendaciones personalizadas.",

                            fontSize =
                                12.sp,

                            color =
                                Color(0xFF78716C),

                            modifier =
                                Modifier.padding(
                                    top = 2.dp,
                                    bottom = 12.dp
                                )
                        )

                        FlowRow(

                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp),

                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            viewModel
                                .availablePreferences
                                .forEach { preference ->

                                    val isSelected =
                                        uiState
                                            .selectedPreferences
                                            .contains(
                                                preference
                                            )

                                    FilterChip(

                                        selected =
                                            isSelected,

                                        onClick = {

                                            viewModel
                                                .togglePreference(
                                                    preference
                                                )
                                        },

                                        label = {

                                            Text(
                                                text =
                                                    preference,

                                                fontSize =
                                                    12.sp,

                                                fontWeight =
                                                    if (
                                                        isSelected
                                                    ) {
                                                        FontWeight.Bold
                                                    } else {
                                                        FontWeight.Normal
                                                    }
                                            )
                                        },

                                        leadingIcon =
                                            if (
                                                isSelected
                                            ) {

                                                {

                                                    Icon(
                                                        imageVector =
                                                            Icons.Default.Check,

                                                        contentDescription =
                                                            null,

                                                        modifier =
                                                            Modifier.size(
                                                                16.dp
                                                            )
                                                    )
                                                }

                                            } else {
                                                null
                                            },

                                        colors =
                                            FilterChipDefaults
                                                .filterChipColors(

                                                    selectedContainerColor =
                                                        PeruGold40
                                                            .copy(
                                                                alpha =
                                                                    0.18f
                                                            ),

                                                    selectedLabelColor =
                                                        PeruGold40,

                                                    selectedLeadingIconColor =
                                                        PeruGold40
                                                )
                                    )
                                }
                        }

                        if (
                            uiState.preferencesError != null
                        ) {

                            Text(
                                text =
                                    uiState
                                        .preferencesError!!,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .error,

                                fontSize =
                                    12.sp,

                                modifier =
                                    Modifier.padding(
                                        top = 8.dp
                                    )
                            )
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )

                // =================================================
                // GUARDAR
                // =================================================

                Button(

                    onClick = {
                        viewModel.saveProfile()
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp),

                    enabled =
                        !uiState.isSaving &&
                                !uiState.isUploadingPhoto,

                    shape =
                        RoundedCornerShape(16.dp),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                PeruGold40
                        )

                ) {

                    if (
                        uiState.isSaving
                    ) {

                        CircularProgressIndicator(
                            color =
                                Color.White,

                            modifier =
                                Modifier.size(
                                    24.dp
                                )
                        )

                    } else {

                        Text(
                            text =
                                "Guardar Cambios en mi Perfil",

                            fontSize =
                                16.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                Color.White
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(28.dp)
                )
            }
        }
    }
}
