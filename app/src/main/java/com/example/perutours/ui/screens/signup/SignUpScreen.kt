package com.example.perutours.ui.theme.screens.signup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.perutours.ui.theme.*

data class RoleOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun SignUpScreen(
    onSignUpSuccess: (selectedRole: String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRoleId by remember { mutableStateOf("cliente") }

    val roles = listOf(
        RoleOption("cliente", "Cliente / Turista", "Explora, cotiza y reserva viajes", Icons.Default.Explore, RoleTouristAccent),
        RoleOption("atencion", "Atención Turística", "Revisa solicitudes y deriva a agentes", Icons.Default.SupportAgent, RoleSupportAccent),
        RoleOption("agente", "Agente Turístico", "Elabora presupuestos y reservas", Icons.Default.Badge, RoleAgentAccent),
        RoleOption("admin", "Administrador", "Proveedores y validación de pagos", Icons.Default.Shield, RoleAdminAccent),
        RoleOption("gerente", "Gerente Comercial", "Métricas y analítica gerencial", Icons.Default.BarChart, RoleManagerAccent)
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("Crear tu cuenta", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Ingresa tus datos y selecciona tu rol en PeruTours.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Datos personales
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre completo") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PeruGold40) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo electrónico") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PeruGold40) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Teléfono celular (+51)") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = PeruGold40) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña segura") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PeruGold40) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            // Selector de Rol con tarjetas
            Spacer(modifier = Modifier.height(24.dp))
            Text("Selecciona tu rol en la plataforma:", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            roles.forEach { role ->
                val isSelected = selectedRoleId == role.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clickable { selectedRoleId = role.id },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) role.color.copy(alpha = 0.12f) else SurfaceLight
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) role.color else Color(0xFFE7E5E4)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = role.color.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(role.icon, contentDescription = null, tint = role.color)
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(role.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(role.subtitle, fontSize = 12.sp, color = Color(0xFF78716C))
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedRoleId = role.id },
                            colors = RadioButtonDefaults.colors(selectedColor = role.color)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onSignUpSuccess(selectedRoleId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PeruGold40)
            ) {
                Text("Registrarse y Comenzar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center, // ✅ Correcto para centrar en un Row
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("¿Ya tienes cuenta? ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                TextButton(onClick = onNavigateToLogin) {
                    Text("Inicia sesión", color = PeruGold40, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}