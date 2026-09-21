package com.perutours.smarttravel.presentation.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Orange = Color(0xFFE8611A)
private val OrangeLight = Color(0xFFFFF3ED)

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToVerifyEmail: () -> Unit
) {
    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (uiState) {
            is RegisterUiState.Success -> onNavigateToVerifyEmail()
            is RegisterUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as RegisterUiState.Error).message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (formState.currentStep != RegisterStep.PERSONAL_DATA) {
                    IconButton(onClick = viewModel::previousStep) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PeruTours",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Orange
                    )
                    Text(
                        text = "Smart Travel",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(48.dp))
            }

            StepIndicator(
                currentStep = formState.currentStep,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                AnimatedContent(
                    targetState = formState.currentStep,
                    transitionSpec = {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    },
                    label = "step_transition"
                ) { step ->
                    when (step) {
                        RegisterStep.PERSONAL_DATA -> RegisterStep1Screen(
                            formState = formState,
                            onFullNameChanged = viewModel::onFullNameChanged,
                            onEmailChanged = viewModel::onEmailChanged,
                            onPhoneChanged = viewModel::onPhoneChanged,
                            onPasswordChanged = viewModel::onPasswordChanged,
                            onTogglePasswordVisibility = viewModel::togglePasswordVisibility
                        )
                        RegisterStep.ROLE -> RegisterStep2Screen(
                            selectedRole = formState.selectedRole,
                            onRoleSelected = viewModel::onRoleSelected
                        )
                        RegisterStep.PREFERENCES -> RegisterStep3Screen(
                            selectedPreferences = formState.selectedPreferences,
                            onPreferenceToggled = viewModel::onPreferenceToggled
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                val isLoading = uiState is RegisterUiState.Loading
                val isLastStep = formState.currentStep == RegisterStep.PREFERENCES

                Button(
                    onClick = {
                        if (formState.currentStep == RegisterStep.PERSONAL_DATA) {
                            if (viewModel.validateStep1()) {
                                viewModel.nextStep()
                            }
                        } else if (isLastStep) {
                            viewModel.register()
                        } else {
                            viewModel.nextStep()
                        }
                    },
                    enabled = !isLoading && viewModel.isStepValid(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isLastStep) "Crear Cuenta e Ingresar" else "Siguiente",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        if (isLastStep) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        viewModel.clearForm()
                        onNavigateToLogin()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "¿Ya tienes cuenta? Inicia sesión",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(currentStep: RegisterStep, modifier: Modifier = Modifier) {
    val steps = RegisterStep.entries
    val currentIndex = currentStep.ordinal

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        steps.forEachIndexed { index, step ->
            val isActive = index <= currentIndex

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Orange else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            )

            if (index < steps.lastIndex) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(if (index < currentIndex) Orange else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}
