package com.example.perutours.ui.screens.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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
import com.example.perutours.ui.theme.BackgroundLight
import com.example.perutours.ui.theme.PeruGold40
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    auth: FirebaseAuth,
    navigateToSignUp: () -> Unit,
    navigateToHome: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessMessage by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "¡Bienvenido de vuelta!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Ingresa tus credenciales para acceder a tu panel turístico.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 28.dp)
                )

                // Campo Correo Electrónico
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        infoMessage = null
                    },
                    label = { Text("Correo electrónico") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = PeruGold40
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo Contraseña
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        infoMessage = null
                    },
                    label = { Text("Contraseña") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = PeruGold40
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                // Mensajes de error o éxito
                if (infoMessage != null) {
                    Text(
                        text = infoMessage ?: "",
                        color = if (isSuccessMessage) Color(0xFF15803D) else MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // CRITERIO 4: Recuperación de contraseña por correo
                TextButton(
                    onClick = {
                        if (email.isBlank()) {
                            isSuccessMessage = false
                            infoMessage = "Por favor ingresa tu correo arriba para enviarte el enlace de recuperación."
                        } else {
                            auth.sendPasswordResetEmail(email.trim())
                                .addOnSuccessListener {
                                    isSuccessMessage = true
                                    infoMessage = "Te hemos enviado un enlace a $email para restablecer tu contraseña."
                                }
                                .addOnFailureListener { e ->
                                    isSuccessMessage = false
                                    infoMessage = "Error al solicitar recuperación: ${e.localizedMessage}"
                                }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                ) {
                    Text("¿Olvidaste tu contraseña?", color = PeruGold40, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Botón Iniciar Sesión con VALIDACIÓN DE CORREO (Criterio 2)
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            isSuccessMessage = false
                            infoMessage = "Por favor ingresa tu correo y contraseña"
                        } else {
                            isLoading = true
                            isSuccessMessage = false
                            auth.signInWithEmailAndPassword(email.trim(), password.trim())
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        val user = auth.currentUser

                                        // 👉 CRITERIO 2: Bloquear si no está verificado
                                        if (user != null && user.isEmailVerified) {
                                            navigateToHome()
                                        } else {
                                            auth.signOut() // Cierra sesión inmediatamente
                                            isSuccessMessage = false
                                            infoMessage = "Debes verificar tu correo antes de ingresar. Revisa tu bandeja de entrada o spam."
                                        }
                                    } else {
                                        val errorMsg = task.exception?.localizedMessage ?: ""
                                        isSuccessMessage = false
                                        infoMessage = when {
                                            errorMsg.contains("badly formatted", true) -> "El formato de correo no es válido."
                                            errorMsg.contains("invalid-credential", true) -> "Correo o contraseña incorrectos."
                                            else -> "Error al iniciar sesión. Revisa tus datos."
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
                            text = "Iniciar sesión",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Divisor
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFE7E5E4),
                        thickness = 1.dp
                    )
                    Text(
                        text = "o continúa con",
                        fontSize = 12.sp,
                        color = Color(0xFF78716C),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFE7E5E4),
                        thickness = 1.dp
                    )
                }

                // Botón Google
                OutlinedButton(
                    onClick = { navigateToHome() },
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
                            text = "Continuar con Google",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1917)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Enlace a Registro
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "¿Aún no tienes cuenta? ",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    TextButton(onClick = navigateToSignUp) {
                        Text(
                            text = "Regístrate aquí",
                            color = PeruGold40,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}