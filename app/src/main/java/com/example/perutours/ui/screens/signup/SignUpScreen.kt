package com.example.perutours.ui.screens.signup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.perutours.R
import com.example.perutours.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest

data class RoleOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    auth: FirebaseAuth,
    navigateToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedRoleId by remember { mutableStateOf("cliente") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val roles = listOf(
        RoleOption("cliente", "Cliente / Turista", "Explora, cotiza y reserva viajes", "🧭", RoleTouristAccent),
        RoleOption("atencion", "Atención Turística", "Revisa solicitudes y asesora clientes", "🎧", RoleSupportAccent),
        RoleOption("agente", "Agente Turístico", "Elabora cotizaciones y reservas", "📋", RoleAgentAccent),
        RoleOption("admin", "Administrador", "Proveedores y validación de pagos", "🛡️", RoleAdminAccent),
        RoleOption("gerente", "Gerente Comercial", "Métricas y analítica gerencial", "📊", RoleManagerAccent)
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

            Text(
                text = "Crear tu cuenta",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Ingresa tus datos y selecciona tu rol en PeruTours.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 22.dp)
            )

            // Nombre
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorMessage = null },
                label = { Text("Nombre completo") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PeruGold40) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Correo
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                label = { Text("Correo electrónico") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PeruGold40) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Teléfono
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; errorMessage = null },
                label = { Text("Teléfono celular (+51)") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = PeruGold40) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Contraseña
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PeruGold40) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Selector de Roles (Tarjetas seleccionables)
            Text(
                text = "Selecciona tu rol en la agencia:",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            roles.forEach { role ->
                val isSelected = selectedRoleId == role.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { selectedRoleId = role.id },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) role.color.copy(alpha = 0.10f) else SurfaceLight
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
                            color = role.color.copy(alpha = 0.18f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(role.emoji, fontSize = 20.sp)
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

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botón de Registro
            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank() || password.isBlank()) {
                        errorMessage = "Por favor completa todos los campos"
                    } else if (password.length < 6) {
                        errorMessage = "La contraseña debe tener al menos 6 caracteres"
                    } else {
                        isLoading = true
                        auth.createUserWithEmailAndPassword(email.trim(), password.trim())
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    // Guarda el nombre del usuario
                                    val profileUpdates = userProfileChangeRequest {
                                        displayName = name.trim()
                                    }
                                    user?.updateProfile(profileUpdates)
                                    navigateToLogin()
                                } else {
                                    errorMessage = task.exception?.localizedMessage ?: "Error al registrarse"
                                }
                            }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PeruGold40)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "Registrarse y Comenzar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Separador
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE7E5E4), thickness = 1.dp)
                Text(
                    text = "o continúa con",
                    fontSize = 12.sp,
                    color = Color(0xFF78716C),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE7E5E4), thickness = 1.dp)
            }

            // Botón Google
            OutlinedButton(
                onClick = { navigateToLogin() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE7E5E4))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.google),
                        contentDescription = "Google",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Registrarse con Google",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1917)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pie con enlace a Login
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿Ya tienes cuenta? ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                TextButton(onClick = navigateToLogin) {
                    Text(
                        text = "Inicia sesión",
                        color = PeruGold40,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}