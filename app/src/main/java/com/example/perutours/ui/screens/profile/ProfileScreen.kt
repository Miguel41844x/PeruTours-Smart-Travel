package com.example.perutours.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.perutours.ui.theme.BackgroundLight
import com.example.perutours.ui.theme.PeruGold40
import com.example.perutours.ui.theme.SurfaceLight
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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

@OptIn(ExperimentalMaterial3Api::class)
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

    // Estados de carga
    var isLoadingInitialData by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

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
                Spacer(modifier = Modifier.height(12.dp))

                // SECCIÓN 1: DATOS PERSONALES
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

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nombre completo *") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PeruGold40)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Teléfono celular (9 dígitos) *") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = PeruGold40)
                            },
                            prefix = { Text("+51 ") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("Ciudad / País de origen *") },
                            leadingIcon = {
                                Icon(Icons.Default.LocationCity, contentDescription = null, tint = PeruGold40)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

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

                Spacer(modifier = Modifier.height(24.dp))

                // BOTÓN GUARDAR
                Button(
                    onClick = {
                        isSaving = true
                        val userProfileData = hashMapOf(
                            "uid" to uid,
                            "name" to name.trim(),
                            "email" to (currentUser?.email ?: ""),
                            "phone" to phone.trim(),
                            "dni" to dniOrPassport.trim(),
                            "city" to city.trim(),
                            "role" to role,
                            "updatedAt" to System.currentTimeMillis()
                        )

                        firestore.collection("users").document(uid)
                            .set(userProfileData, SetOptions.merge())
                            .addOnSuccessListener {
                                val profileUpdates = userProfileChangeRequest {
                                    displayName = "${name.trim()} | $role | ${phone.trim()}"
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
                    enabled = !isSaving,
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