package com.example.perutours.ui.screens.signup

import android.util.Patterns
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var selectedRoleId by remember { mutableStateOf("cliente") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showVerificationDialog by remember { mutableStateOf(false) }

    val roles = listOf(
        RoleOption("cliente", "Cliente / Turista", "Explora, cotiza y reserva viajes", "🧭", RoleTouristAccent),
        RoleOption("atencion", "Atención Turística", "Revisa solicitudes y asesora clientes", "🎧", RoleSupportAccent),
        RoleOption("agente", "Agente Turístico", "Elabora cotizaciones y reservas", "📋", RoleAgentAccent),
        RoleOption("admin", "Administrador", "Proveedores y validación de pagos", "🛡️", RoleAdminAccent),
        RoleOption("gerente", "Gerente Comercial", "Métricas y analítica gerencial", "📊", RoleManagerAccent)
    )

    // Diálogo informativo Criterio 2
    if (showVerificationDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("¡Verifica tu correo!", fontWeight = FontWeight.Bold) },
            text = {
                Text("Hemos enviado un correo a $email. Por favor valida tu cuenta en tu bandeja de entrada o spam antes de iniciar sesión.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showVerificationDialog = false
                        navigateToLogin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PeruGold40)
                ) {
                    Text("Ir a Iniciar Sesión", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = BackgroundLight) {
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
                text = "Completa tus datos reales para tu credencial en PeruTours.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // 1. Nombre
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

            // 2. Correo con validación de formato
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

            // 3. Teléfono de 9 dígitos
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    if (it.length <= 9 && it.all { char -> char.isDigit() }) phone = it
                    errorMessage = null
                },
                label = { Text("Teléfono celular (9 dígitos)") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = PeruGold40) },
                prefix = { Text("+51 ") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Contraseña
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("Contraseña (mínimo 6 caracteres)") },
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

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Confirmar Contraseña
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = null },
                label = { Text("Confirmar contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PeruGold40) },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Selector de Roles
            Text(
                text = "Selecciona tu rol en PeruTours:",
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
                    shape = RoundedCornerShape(14.dp),
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
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = role.color.copy(alpha = 0.20f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(role.emoji, fontSize = 18.sp)
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(role.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(role.subtitle, fontSize = 11.sp, color = Color(0xFF78716C))
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
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botón de Registro con validaciones completas y envío de correo
            Button(
                onClick = {
                    val cleanEmail = email.trim()
                    val cleanPass = password.trim()
                    val cleanConfirm = confirmPassword.trim()
                    val cleanName = name.trim()

                    when {
                        cleanName.isBlank() || cleanEmail.isBlank() || phone.isBlank() || cleanPass.isBlank() -> {
                            errorMessage = "Por favor completa todos los campos obligatorios."
                        }
                        !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> {
                            errorMessage = "El formato del correo electrónico no es válido."
                        }
                        phone.length != 9 || !phone.startsWith("9") -> {
                            errorMessage = "Ingresa un celular válido de 9 dígitos que comience con 9."
                        }
                        cleanPass.length < 6 -> {
                            errorMessage = "La contraseña debe tener al menos 6 caracteres."
                        }
                        cleanPass != cleanConfirm -> {
                            errorMessage = "Las contraseñas no coinciden."
                        }
                        else -> {
                            isLoading = true
                            auth.createUserWithEmailAndPassword(cleanEmail, cleanPass)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val user = auth.currentUser
                                        val profileUpdates = userProfileChangeRequest {
                                            displayName = "$cleanName | $selectedRoleId | $phone"
                                        }

                                        // Guarda el nombre y luego envía el correo de verificación
                                        user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                                            user.sendEmailVerification().addOnCompleteListener {
                                                isLoading = false
                                                auth.signOut()
                                                showVerificationDialog = true
                                            }
                                        }
                                    } else {
                                        isLoading = false
                                        val errorMsg = task.exception?.localizedMessage ?: ""
                                        errorMessage = when {
                                            errorMsg.contains("already in use", true) -> "Este correo electrónico ya está registrado."
                                            else -> "Error al registrarse: $errorMsg"
                                        }
                                    }
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

            Spacer(modifier = Modifier.height(18.dp))

            // Enlace a Login
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
        }
    }
}