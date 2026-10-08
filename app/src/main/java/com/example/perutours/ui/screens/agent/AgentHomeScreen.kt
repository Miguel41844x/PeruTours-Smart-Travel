package com.example.perutours.ui.screens.agent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.perutours.ui.theme.BackgroundLight
import com.example.perutours.ui.theme.PeruGold40
import com.example.perutours.ui.theme.SurfaceLight
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentHomeScreen(
    auth: FirebaseAuth,
    onViewRequests: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val currentUser = auth.currentUser
    val uid = currentUser?.uid.orEmpty()
    val rawName = currentUser?.displayName.orEmpty()

    // 1. Estados reactivos para nombre y foto real
    var agentName by remember {
        mutableStateOf(
            rawName.split("|").firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
                ?: currentUser?.email?.substringBefore("@")
                ?: "Agente"
        )
    }
    var photoUrl by remember {
        mutableStateOf(currentUser?.photoUrl?.toString().orEmpty())
    }

    // 2. CONTADORES EN TIEMPO REAL PARA EL RESUMEN DE OPERACIONES
    var pendingCount by remember { mutableStateOf(0) }
    var quotedCount by remember { mutableStateOf(0) }
    var isLoadingCounts by remember { mutableStateOf(true) }

    val agentEmail = currentUser?.email ?: "agente@perutours.pe"
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Sincronizar perfil y contadores desde Firestore en tiempo real
    DisposableEffect(uid) {
        val firestore = FirebaseFirestore.getInstance()

        // Listener A: Perfil del Agente
        val userListener = if (uid.isNotEmpty()) {
            firestore.collection("users").document(uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        snapshot.getString("name")?.takeIf { it.isNotBlank() }?.let {
                            agentName = it
                        }
                        snapshot.getString("photoUrl")?.let {
                            photoUrl = it
                        }
                    }
                }
        } else null

        // Listener B: Conteo de Solicitudes Pendientes vs Cotizadas
        val requestsListener = firestore.collection("travel_requests")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val docs = snapshot.documents
                    pendingCount = docs.count { doc ->
                        val status = doc.getString("status").orEmpty()
                        status.contains("Pendiente", ignoreCase = true)
                    }
                    quotedCount = docs.count { doc ->
                        val status = doc.getString("status").orEmpty()
                        status.equals("Cotizado", ignoreCase = true)
                    }
                    isLoadingCounts = false
                }
            }

        onDispose {
            userListener?.remove()
            requestsListener.remove()
        }
    }

    // Diálogo de confirmación para Cerrar Sesión
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar Sesión", fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas cerrar tu sesión como Agente Turístico?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
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
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        // Foto real o inicial
                        if (photoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Foto de agente",
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape,
                                color = PeruGold40.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = agentName.take(1).uppercase(),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = PeruGold40
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Columna: Nombre -> Correo -> Cuadro de Agente debajo
                        Column {
                            Text(
                                text = "¡Hola, $agentName!",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = agentEmail,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            // Cuadro de Agente debajo del correo
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFFD97706))
                            ) {
                                Text(
                                    text = "Agente Turístico",
                                    color = Color(0xFFB45309),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Badge "En línea"
                    Surface(
                        color = Color(0xFFD1FAE5),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "En línea",
                                color = Color(0xFF065F46),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Botón para Cerrar Sesión
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // HERO CARD DE GESTIÓN DE COTIZACIONES
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFD97706), Color(0xFFB45309))
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column {
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "MÓDULO DE COTIZACIONES",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Gestión de Cotizaciones Turísticas",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Estructura propuestas para los clientes con desglose de servicios, costos unitarios, margen comercial y registro formal con estado 'Cotizado'.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = onViewRequests,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFFB45309)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.RequestQuote,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Bandeja de Solicitudes ($pendingCount pendientes)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // ============================================================
            // RESUMEN DE OPERACIONES CON CANTIDADES DINÁMICAS
            // ============================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Resumen de Operaciones",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // TARJETA 1: PENDIENTES CON CANTIDAD REAL
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onViewRequests() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Falta cotizar",
                                    color = Color(0xFFB45309),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Cantidad en número grande
                        Text(
                            text = if (isLoadingCounts) "..." else "$pendingCount",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFB45309)
                        )
                        Text(
                            text = "Solicitudes pendientes",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // TARJETA 2: COTIZADAS CON CANTIDAD REAL
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onViewRequests() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, Color(0xFFD1FAE5))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFD1FAE5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AssignmentTurnedIn,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Surface(
                                color = Color(0xFFD1FAE5),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Cotizado",
                                    color = Color(0xFF065F46),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Cantidad en número grande
                        Text(
                            text = if (isLoadingCounts) "..." else "$quotedCount",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF059669)
                        )
                        Text(
                            text = "Propuestas emitidas",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // PROTOCOLO COMERCIAL PARA HU05
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                border = BorderStroke(1.dp, Color(0xFFFDE68A))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Protocolo de Elaboración de Cotización",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color(0xFF92400E)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val tips = listOf(
                        "1. Revisa destino, origen y número de viajeros de la solicitud.",
                        "2. Selecciona del catálogo: Hotel, Tren, Entradas y Seguro de viaje.",
                        "3. Agrega servicios personalizados si el turista lo especifica.",
                        "4. Aplica el margen de ganancia comercial (15% recomendado).",
                        "5. Al registrar, la solicitud pasará a 'Cotizado' y enviará una notificación al cliente."
                    )

                    tips.forEach { tip ->
                        Text(
                            text = tip,
                            fontSize = 11.5.sp,
                            color = Color(0xFF78350F),
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}