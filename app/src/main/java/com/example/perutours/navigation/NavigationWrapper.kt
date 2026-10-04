package com.example.perutours.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.perutours.ui.screens.home.HomeScreen
import com.example.perutours.ui.screens.initial.InitialScreen
import com.example.perutours.ui.screens.login.LoginScreen
import com.example.perutours.ui.screens.profile.ProfileScreen
import com.example.perutours.ui.screens.quotation.CreateQuotationScreen
import com.example.perutours.ui.screens.quotation.QuotationDetailScreen
import com.example.perutours.ui.screens.quotation.QuotationUiState
import com.example.perutours.ui.screens.quotation.QuotationViewModel
import com.example.perutours.ui.screens.request.TravelRequestScreen
import com.example.perutours.ui.screens.signup.SignUpScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun NavigationWrapper(
    navHostController: NavHostController,
    auth: FirebaseAuth
) {
    val currentUser = auth.currentUser

    // CRITERIO 3: Persistencia de sesión
    // Solo si el usuario ya inició sesión Y su correo ya está verificado, entra directo a Home
    val startDestination = if (currentUser != null && currentUser.isEmailVerified) {
        "home"
    } else {
        "initial"
    }

    NavHost(
        navController = navHostController,
        startDestination = startDestination
    ) {

        composable(route = "initial") {
            InitialScreen(
                navigateToLogin = { navHostController.navigate(route = "logIn") },
                navigateToSignUp = { navHostController.navigate(route = "signUp") }
            )
        }

        composable(route = "logIn") {
            LoginScreen(
                navigateToSignUp = {
                    navHostController.navigate(route = "signUp")
                },
                navigateToHome = {
                    navHostController.navigate(route = "home") {
                        popUpTo("logIn") { inclusive = true }
                    }
                }
            )
        }

        composable(route = "signUp") {
            SignUpScreen(
                navigateToLogin = {
                    navHostController.navigate(route = "logIn") {
                        popUpTo("signUp") { inclusive = true }
                    }
                }
            )
        }

        composable(route = "home") {
            HomeScreen(
                auth = auth,
                navigateToTravelRequest = {
                    navHostController.navigate(route = "travelRequest")
                },
                navigateToProfile = {
                    navHostController.navigate(route = "profile")
                },
                navigateToInitial = {
                    navHostController.navigate(route = "initial") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Gestión de Perfil y Preferencias
        composable(route = "profile") {
            ProfileScreen(
                onBack = {
                    navHostController.popBackStack()
                }
            )
        }

        composable(route = "travelRequest") {
            TravelRequestScreen(
                onBack = { navHostController.popBackStack() },
                onSaved = {
                    navHostController.navigate(route = "home") {
                        popUpTo("travelRequest") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = "create_quotation/{requestId}") { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            val quotationViewModel: QuotationViewModel = viewModel()

            LaunchedEffect(requestId) {
                quotationViewModel.initFromRequest(
                    requestId = requestId,
                    clientId = "client_demo",
                    clientName = "Cliente",
                    destination = "Cusco & Machu Picchu",
                    travelers = 2
                )
            }

            CreateQuotationScreen(
                viewModel = quotationViewModel,
                onNavigateBack = { navHostController.popBackStack() },
                onQuotationCreated = { quotationId ->
                    navHostController.navigate("quotation_detail/$quotationId") {
                        popUpTo("create_quotation/{requestId}") { inclusive = true }
                    }
                }
            )
        }

        composable(route = "quotation_detail/{quotationId}") { backStackEntry ->
            val quotationId = backStackEntry.arguments?.getString("quotationId") ?: ""
            val quotationViewModel: QuotationViewModel = viewModel()
            val uiState by quotationViewModel.uiState.collectAsState()

            LaunchedEffect(quotationId) {
                quotationViewModel.loadQuotationById(quotationId)
            }

            when (val state = uiState) {
                is QuotationUiState.Success -> {
                    QuotationDetailScreen(
                        quotation = state.quotation,
                        onNavigateBack = { navHostController.popBackStack() },
                        onAcceptQuotation = {
                            navHostController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    )
                }
                is QuotationUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is QuotationUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = state.message)
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
