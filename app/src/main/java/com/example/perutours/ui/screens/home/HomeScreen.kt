package com.example.perutours.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.perutours.R
import com.example.perutours.ui.theme.BackgroundLight
import com.example.perutours.ui.theme.PeruGold40
import com.example.perutours.ui.theme.SurfaceLight
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class TourPackage(
    val title: String,
    val location: String,
    val duration: String,
    val price: String,
    val rating: String,
    val category: String,
    val imageRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    auth: FirebaseAuth,
    navigateToProfile: () -> Unit = {},
    navigateToInitial: () -> Unit
) {
    val currentUser = auth.currentUser
    val uid = currentUser?.uid ?: ""
    val rawName = currentUser?.displayName ?: "Turista"

    var userName by remember {
        mutableStateOf(rawName.split("|").firstOrNull()?.trim() ?: "Turista")
    }
    var photoUrl by remember {
        mutableStateOf(currentUser?.photoUrl?.toString() ?: "")
    }
    var userPreferences by remember {
        mutableStateOf(listOf<String>())
    }
    val userEmail = currentUser?.email ?: ""
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Cargar datos actualizados desde Firestore cada vez que se muestra HomeScreen
    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        snapshot.getString("name")?.takeIf { it.isNotBlank() }?.let { userName = it }
                        snapshot.getString("photoUrl")?.let { photoUrl = it }
                        val prefs = snapshot.get("preferences") as? List<*>
                        if (prefs != null) {
                            userPreferences = prefs.filterIsInstance<String>()
                        }
                    }
                }
        }
    }

    val featuredPackages = listOf(
        TourPackage("Machu Picchu Mágico", "Cusco, Perú", "4 días / 3 noches", "S/ 1,299", "4.9", "Historia y Cultura", R.drawable.machu_picchu),
        TourPackage("Líneas de Nazca y Huacachina", "Ica, Perú", "2 días / 1 noche", "S/ 480", "4.8", "Aventura y Trekking", R.drawable.machu_picchu),
        TourPackage("Cañón del Colca y Arequipa", "Arequipa, Perú", "3 días / 2 noches", "S/ 650", "4.7", "Naturaleza y Selva", R.drawable.machu_picchu)
    )

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar Sesión", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas cerrar tu sesión en PeruTours?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        auth.signOut()
                        navigateToInitial()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cerrar Sesión", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { navigateToProfile() }
                    ) {
                        if (photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Column {
                            Text(
                                text = "Hola, $userName 👋",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (userPreferences.isNotEmpty()) {
                                    "Intereses: ${userPreferences.take(2).joinToString(" • ")}"
                                } else {
                                    userEmail
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Botón para abrir la pantalla de Gestión de Perfil (HU02)
                    IconButton(onClick = navigateToProfile) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Mi Perfil",
                            tint = PeruGold40
                        )
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Tarjeta Personalización de Perfil
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navigateToProfile() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PeruGold40)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Descubre el Perú a tu medida",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Toca aquí para personalizar tu foto de perfil, datos y preferencias de viaje.",
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Paquetes Turísticos Destacados",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            featuredPackages.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, Color(0xFFE7E5E4))
                ) {
                    Column {
                        Image(
                            painter = painterResource(id = item.imageRes),
                            contentDescription = item.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = PeruGold40,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = item.rating,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.padding(top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF78716C),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${item.location} • ${item.duration}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF78716C)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.price,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PeruGold40
                                )
                                Button(
                                    onClick = { },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PeruGold40)
                                ) {
                                    Text("Ver detalle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}