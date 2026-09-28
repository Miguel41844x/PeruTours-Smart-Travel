package com.example.perutours.ui.theme.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.perutours.ui.theme.*

data class DestinationMock(
    val title: String,
    val location: String,
    val price: String,
    val rating: String,
    val days: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String = "Viajero",
    onLogoutClick: () -> Unit
) {
    var selectedNavIndex by remember { mutableIntStateOf(0) }

    val mockDestinations = listOf(
        DestinationMock("Machu Picchu Mágico", "Cusco", "S/ 1,250", "4.9", "4D / 3N"),
        DestinationMock("Huacachina & Paracas", "Ica", "S/ 480", "4.8", "2D / 1N"),
        DestinationMock("Cañón del Colca", "Arequipa", "S/ 720", "4.7", "3D / 2N"),
        DestinationMock("Reserva Pacaya Samiria", "Iquitos", "S/ 1,890", "4.9", "5D / 4N")
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceLight,
                tonalElevation = 8.dp
            ) {
                val navItems = listOf(
                    Triple("Inicio", Icons.Default.Home, 0),
                    Triple("Explorar", Icons.Default.Search, 1),
                    Triple("Cotizaciones", Icons.Default.ReceiptLong, 2),
                    Triple("Reservas", Icons.Default.Event, 3),
                    Triple("Perfil", Icons.Default.Person, 4)
                )

                navItems.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = selectedNavIndex == index,
                        onClick = { selectedNavIndex = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedNavIndex == index) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PeruGold40,
                            selectedTextColor = PeruGold40,
                            indicatorColor = PeruGoldContainerLight
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header con Saludo y Logout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(PeruGoldContainerLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🇵🇪", fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("¡Allianllachu, $userName!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("¿Cuál será tu próxima aventura?", fontSize = 12.sp, color = Color(0xFF78716C))
                    }
                }

                IconButton(onClick = onLogoutClick) {
                    Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión", tint = Color(0xFF78716C))
                }
            }

            // Barra de Búsqueda estilo Airbnb
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = PeruGold40)
                    Spacer(Modifier.width(12.dp))
                    Text("Cusco, Máncora, Tarapoto...", color = Color(0xFFA8A29E), fontSize = 14.sp)
                }
            }

            // Sección Accesos Rápidos
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("Nueva Solicitud", "Mis Viajes", "Favoritos").forEach { tag ->
                    AssistChip(
                        onClick = { },
                        label = { Text(tag, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = PeruGoldContainerLight,
                            labelColor = OnPeruGoldContainerLight
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Carrusel de Destinos Populares
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Destinos Destacados",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(mockDestinations) { item ->
                    Card(
                        modifier = Modifier
                            .width(220.dp)
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Column {
                            // Placeholder de foto
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(Color(0xFFE7E5E4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Landscape, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                            }

                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${item.location} • ${item.days}", fontSize = 12.sp, color = Color(0xFF78716C))
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Desde ${item.price}", fontWeight = FontWeight.ExtraBold, color = PeruGold40, fontSize = 14.sp)
                                    Text("★ ${item.rating}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}