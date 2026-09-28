package com.example.perutours.ui.screens.profile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import coil.compose.AsyncImage
import com.example.perutours.ui.theme.BackgroundLight
import com.example.perutours.ui.theme.PeruGold40
import com.example.perutours.ui.theme.SurfaceLight
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.io.File

// Función auxiliar para crear un archivo temporal seguro donde la cámara guardará la foto
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
    auth: FirebaseAuth,
    navigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser = auth.currentUser
    val uid = currentUser?.uid ?: ""
    val firestore = remember { FirebaseFirestore.getInstance() }
    val storage = remember { FirebaseStorage.getInstance() }

    // Parseo inicial compatible con HU01 ("Nombre | rol | telefono")
    val rawDisplay = currentUser?.displayName ?: ""
    val parts = rawDisplay.split("|").map { it.trim() }
    val initialName = parts.getOrNull(0) ?: ""
    val initialRole = parts.getOrNull(1) ?: "cliente"
    val initialPhone = parts.getOrNull(2) ?: ""

    // Estados del formulario de perfil
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var dniOrPassport by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Lima, Perú") }
    var role by remember { mutableStateOf(initialRole) }
    var photoUrl by remember { mutableStateOf(currentUser?.photoUrl?.toString() ?: "") }
    var localPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Catálogo de preferencias turísticas para personalizar la experiencia
    val availablePreferences = listOf(
        "Aventura y Trekking",
        "Historia y Cultura",
        "Naturaleza y Selva",
        "Gastronomía Peruana",
        "Playas y Relax",
        "Turismo Vivencial",
        "Fotografía y Paisajes",
        "Viaje en Familia"
    )
    var selectedPreferences by remember {
        mutableStateOf(setOf("Historia y Cultura", "Gastronomía Peruana"))
    }

    // Estados de validación Material Design (TextField isError + supportingText)
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var cityError by remember { mutableStateOf<String?>(null) }
    var preferencesError by remember { mutableStateOf<String?>(null) }

    // Estados de carga, subida y modal de selección de cámara/galería
    var isLoadingInitialData by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var showPhotoSourceSheet by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    // 1. Cargar datos persistidos en Firestore al abrir la pantalla
    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        doc.getString("name")?.takeIf { it.isNotBlank() }?.let { name = it }
                        doc.getString("phone")?.takeIf { it.isNotBlank() }?.let { phone = it }
                        doc.getString("dni")?.let { dniOrPassport = it }
                        doc.getString("city")?.takeIf { it.isNotBlank() }?.let { city = it }
                        doc.getString("role")?.takeIf { it.isNotBlank() }?.let { role = it }
                        doc.getString("photoUrl")?.takeIf { it.isNotBlank() }?.let { photoUrl = it }

                        val savedPrefs = doc.get("preferences") as? List<*>
                        if (!savedPrefs.isNullOrEmpty()) {
                            selectedPreferences = savedPrefs.filterIsInstance<String>().toSet()
                        }
                    }
                    isLoadingInitialData = false
                }
                .addOnFailureListener {
                    isLoadingInitialData = false
                }
        } else {
            isLoadingInitialData = false
        }
    }

    // Función que sube la foto a Firebase Storage y guarda el link en Firestore
    fun uploadProfilePhotoToFirebase(uri: Uri) {
        if (uid.isEmpty()) return
        localPhotoUri = uri
        isUploadingPhoto = true

        val photoRef = storage.reference.child("profile_pictures/$uid.jpg")
        photoRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                photoRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                val remoteUrl = downloadUri.toString()
                photoUrl = remoteUrl

                // Actualizar foto en FirebaseAuth
                val profileUpdates = userProfileChangeRequest {
                    this.photoUri = downloadUri
                }
                currentUser?.updateProfile(profileUpdates)

                // Guardar URL inmediatamente en Firestore
                firestore.collection("users").document(uid)
                    .set(mapOf("photoUrl" to remoteUrl), SetOptions.merge())
                    .addOnCompleteListener {
                        isUploadingPhoto = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Foto de perfil guardada en Firebase Storage")
                        }
                    }
            }
            .addOnFailureListener { e ->
                isUploadingPhoto = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Error al subir la foto: ${e.localizedMessage}")
                }
            }
    }

    // 2. Launcher para elegir foto de la Galería (Photo Picker oficial de Android)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadProfilePhotoToFirebase(uri)
        }
    }

    // 3. Launcher para tomar foto con la Cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val capturedUri = pendingCameraUri
        if (success && capturedUri != null) {
            uploadProfilePhotoToFirebase(capturedUri)
        }
    }

    // 4. Launcher para pedir permiso de Cámara en tiempo de ejecución
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val newUri = createTempImageUri(context)
            pendingCameraUri = newUri
            cameraLauncher.launch(newUri)
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Permiso de cámara denegado. Habilítalo para tomar tu foto.")
            }
        }
    }

    // BottomSheet para elegir entre Cámara o Galería
    if (showPhotoSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoSourceSheet = false },
            containerColor = SurfaceLight
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Actualizar foto de perfil",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Selecciona cómo deseas subir tu fotografía a PeruTours",
                    fontSize = 13.sp,
                    color = Color(0xFF78716C),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Opción 1: Tomar foto con la Cámara
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showPhotoSourceSheet = false
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                val newUri = createTempImageUri(context)
                                pendingCameraUri = newUri
                                cameraLauncher.launch(newUri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = PeruGold40
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Tomar foto con la Cámara", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Captura una nueva foto en este momento", fontSize = 12.sp, color = Color(0xFF78716C))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Opción 2: Seleccionar desde la Galería
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showPhotoSourceSheet = false
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = PeruGold40
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Seleccionar desde Galería", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Elige una imagen guardada en tu teléfono", fontSize = 12.sp, color = Color(0xFF78716C))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mi Perfil y Preferencias",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        if (isLoadingInitialData) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PeruGold40)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // SECCIÓN 1: FOTO DE PERFIL CON CÁMARA / GALERÍA
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    val imageModel: Any? = localPhotoUri ?: photoUrl.takeIf { it.isNotBlank() }

                    if (imageModel != null) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .size(112.dp)
                                .clip(CircleShape)
                                .border(3.dp, PeruGold40, CircleShape)
                                .clickable { showPhotoSourceSheet = true },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier
                                .size(112.dp)
                                .clip(CircleShape)
                                .border(3.dp, PeruGold40, CircleShape)
                                .clickable { showPhotoSourceSheet = true },
                            color = PeruGold40.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = name.firstOrNull()?.uppercase() ?: "P",
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PeruGold40
                                )
                            }
                        }
                    }

                    // Botón flotante de cámara sobre el avatar
                    SmallFloatingActionButton(
                        onClick = { showPhotoSourceSheet = true },
                        containerColor = PeruGold40,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp)
                    ) {
                        if (isUploadingPhoto) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Cambiar foto",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                TextButton(onClick = { showPhotoSourceSheet = true }) {
                    Text(
                        text = if (isUploadingPhoto) "Subiendo foto a Firebase Storage..." else "Cambiar foto (Cámara o Galería)",
                        color = PeruGold40,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // SECCIÓN 2: DATOS PERSONALES CON VALIDACIÓN MATERIAL 3
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, Color(0xFFE7E5E4))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Información Personal",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Los campos marcados con (*) son obligatorios.",
                            fontSize = 12.sp,
                            color = Color(0xFF78716C),
                            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                        )

                        // Campo obligatorio 1: Nombre completo
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                nameError = null
                            },
                            label = { Text("Nombre completo *") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PeruGold40)
                            },
                            isError = nameError != null,
                            supportingText = {
                                if (nameError != null) {
                                    Text(text = nameError!!, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Campo obligatorio 2: Teléfono celular (9 dígitos)
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { input ->
                                if (input.length <= 9 && input.all { it.isDigit() }) {
                                    phone = input
                                    phoneError = null
                                }
                            },
                            label = { Text("Teléfono celular (9 dígitos) *") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = PeruGold40)
                            },
                            prefix = { Text("+51 ") },
                            isError = phoneError != null,
                            supportingText = {
                                if (phoneError != null) {
                                    Text(text = phoneError!!, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Campo obligatorio 3: Ciudad de origen
                        OutlinedTextField(
                            value = city,
                            onValueChange = {
                                city = it
                                cityError = null
                            },
                            label = { Text("Ciudad / País de origen *") },
                            leadingIcon = {
                                Icon(Icons.Default.LocationCity, contentDescription = null, tint = PeruGold40)
                            },
                            isError = cityError != null,
                            supportingText = {
                                if (cityError != null) {
                                    Text(text = cityError!!, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Campo opcional: DNI o Pasaporte
                        OutlinedTextField(
                            value = dniOrPassport,
                            onValueChange = { dniOrPassport = it },
                            label = { Text("DNI / Pasaporte (Opcional)") },
                            leadingIcon = {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = PeruGold40)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Correo electrónico (Solo lectura - autenticado en Firebase)
                        OutlinedTextField(
                            value = currentUser?.email ?: "",
                            onValueChange = { },
                            enabled = false,
                            label = { Text("Correo verificado") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF78716C))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SECCIÓN 3: PREFERENCIAS DE VIAJE (CHIPS INTERACTIVOS)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (preferencesError != null) MaterialTheme.colorScheme.error else Color(0xFFE7E5E4)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Mis Preferencias de Viaje *",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Elige al menos 1 interés para recibir recomendaciones personalizadas.",
                            fontSize = 12.sp,
                            color = Color(0xFF78716C),
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availablePreferences.forEach { preference ->
                                val isSelected = selectedPreferences.contains(preference)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        preferencesError = null
                                        selectedPreferences = if (isSelected) {
                                            selectedPreferences - preference
                                        } else {
                                            selectedPreferences + preference
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = preference,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PeruGold40.copy(alpha = 0.18f),
                                        selectedLabelColor = PeruGold40,
                                        selectedLeadingIconColor = PeruGold40
                                    )
                                )
                            }
                        }

                        if (preferencesError != null) {
                            Text(
                                text = preferencesError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // BOTÓN GUARDAR CON VALIDACIÓN Y PERSISTENCIA EN FIRESTORE
                Button(
                    onClick = {
                        val cleanName = name.trim()
                        val cleanPhone = phone.trim()
                        val cleanCity = city.trim()

                        var hasValidationError = false

                        if (cleanName.length < 3) {
                            nameError = "Ingresa tu nombre completo (mínimo 3 caracteres)."
                            hasValidationError = true
                        }
                        if (cleanPhone.length != 9 || !cleanPhone.startsWith("9")) {
                            phoneError = "Ingresa un celular válido de 9 dígitos que empiece con 9."
                            hasValidationError = true
                        }
                        if (cleanCity.isBlank()) {
                            cityError = "La ciudad o país de origen es obligatorio."
                            hasValidationError = true
                        }
                        if (selectedPreferences.isEmpty()) {
                            preferencesError = "Selecciona al menos una preferencia de viaje."
                            hasValidationError = true
                        }

                        if (hasValidationError) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Por favor corrige los campos obligatorios marcados en rojo.")
                            }
                            return@Button
                        }

                        // Si todo es válido, guardar en Cloud Firestore y sincronizar con FirebaseAuth
                        isSaving = true
                        val userProfileData = hashMapOf(
                            "uid" to uid,
                            "name" to cleanName,
                            "email" to (currentUser?.email ?: ""),
                            "phone" to cleanPhone,
                            "dni" to dniOrPassport.trim(),
                            "city" to cleanCity,
                            "role" to role,
                            "photoUrl" to photoUrl,
                            "preferences" to selectedPreferences.toList(),
                            "updatedAt" to System.currentTimeMillis()
                        )

                        firestore.collection("users").document(uid)
                            .set(userProfileData, SetOptions.merge())
                            .addOnSuccessListener {
                                // También sincronizamos displayName en FirebaseAuth para mantener compatibilidad con HomeScreen
                                val profileUpdates = userProfileChangeRequest {
                                    displayName = "$cleanName | $role | $cleanPhone"
                                }
                                currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener {
                                    isSaving = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("¡Perfil y preferencias guardados en Firestore con éxito!")
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                isSaving = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Error al guardar en Firestore: ${e.localizedMessage}")
                                }
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = !isSaving && !isUploadingPhoto,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PeruGold40)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Guardar Cambios en mi Perfil",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}