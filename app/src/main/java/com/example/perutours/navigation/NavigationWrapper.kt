package com.example.perutours.navigation

import android.net.Uri
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
import org.koin.androidx.compose.koinViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.perutours.ui.screens.home.HomeScreen
import com.example.perutours.ui.screens.initial.InitialScreen
import com.example.perutours.ui.screens.login.LoginScreen
import com.example.perutours.ui.screens.profile.ProfileScreen
import com.example.perutours.ui.screens.quotation.CreateQuotationScreen
import com.example.perutours.ui.screens.quotation.CustomerQuotationViewModel
import com.example.perutours.ui.screens.quotation.CustomerQuotationsScreen
import com.example.perutours.ui.screens.quotation.QuotationDetailScreen
import com.example.perutours.ui.screens.quotation.QuotationResponseUiState
import com.example.perutours.ui.screens.quotation.QuotationUiState
import com.example.perutours.ui.screens.quotation.QuotationViewModel
import com.example.perutours.ui.screens.reservation.CustomerReservationViewModel
import com.example.perutours.ui.screens.reservation.CustomerReservationsScreen
import com.example.perutours.ui.screens.reservation.ReservationDetailScreen
import com.example.perutours.ui.screens.request.TravelRequestScreen
import com.example.perutours.ui.screens.signup.SignUpScreen
import com.example.perutours.ui.screens.payment.PaymentScreen
import com.example.perutours.service.MyFirebaseMessagingService
import com.example.perutours.data.messaging.FCMTokenManager
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.google.firebase.auth.FirebaseAuth
import com.example.perutours.ui.screens.agent.AgentHomeScreen
import com.example.perutours.ui.screens.admin.AdminDashboardScreen
import com.example.perutours.ui.screens.admin.destination.AdminDestinationFormScreen
import com.example.perutours.ui.screens.admin.destination.AdminDestinationsScreen
import com.example.perutours.ui.screens.admin.provider.AdminProvidersScreen
import com.example.perutours.ui.screens.agent.AgentPaymentDetailScreen
import com.example.perutours.ui.screens.agent.AgentPaymentsScreen
import com.example.perutours.ui.screens.agent.AgentRequestsScreen
import com.example.perutours.ui.screens.sales.SalesDashboardScreen
import org.koin.compose.koinInject

@Composable
fun NavigationWrapper(
    navHostController: NavHostController,
    auth: FirebaseAuth = koinInject(),
    pendingQuotationId: String? = null,
    pendingReservationId: String? = null,
    pendingNotificationType: String? = null,
    onPendingQuotationConsumed: () -> Unit = {}
) {
    val currentUser = auth.currentUser

    // CRITERIO 3: Persistencia de sesión
    // Solo si el usuario ya inició sesión Y su correo ya está verificado, entra directo a Home
    val startDestination =
        if (currentUser != null && currentUser.isEmailVerified) {
            "role_router"
        } else {
            "initial"
        }

    LaunchedEffect(
        pendingQuotationId,
        pendingReservationId,
        pendingNotificationType,
        currentUser?.uid
    ) {
        if (currentUser?.isEmailVerified != true) return@LaunchedEffect

        // HU07: la notificación de revisión de pago abre la reserva del
        // cliente para ver el resultado del comprobante.
        val paymentReservationId = pendingReservationId?.takeIf { it.isNotBlank() }
        if (
            pendingNotificationType == MyFirebaseMessagingService.TYPE_PAYMENT_REVIEW &&
            paymentReservationId != null
        ) {
            navHostController.navigate("customer_reservation/$paymentReservationId") {
                launchSingleTop = true
            }
            onPendingQuotationConsumed()
            return@LaunchedEffect
        }

        val quotationId = pendingQuotationId?.takeIf { it.isNotBlank() }
        if (quotationId != null) {
            val route = if (
                pendingNotificationType == MyFirebaseMessagingService.TYPE_QUOTATION_RESPONSE
            ) {
                "agent_quotation_detail/$quotationId"
            } else {
                "customer_quotation/$quotationId"
            }
            navHostController.navigate(route) {
                launchSingleTop = true
            }
            onPendingQuotationConsumed()
        }
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
                navigateBack = { navHostController.popBackStack() },
                navigateToSignUp = {
                    navHostController.navigate(route = "signUp")
                },
                navigateToHome = {
                    navHostController.navigate(route = "role_router") {
                        popUpTo("logIn") {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(route = "signUp") {
            SignUpScreen(
                navigateBack = { navHostController.popBackStack() },
                navigateToLogin = {
                    navHostController.navigate(route = "logIn") {
                        popUpTo("signUp") { inclusive = true }
                    }
                }
            )
        }

        composable(route = "role_router") {
            RoleRouterScreen(
                navController = navHostController,
                auth = auth
            )
        }

        composable(route = "sales_dashboard") {
            val fcmTokenManager: FCMTokenManager = koinInject()
            val coroutineScope = rememberCoroutineScope()
            SalesDashboardScreen(
                onLogout = {
                    coroutineScope.launch {
                        withTimeoutOrNull(3_000) { fcmTokenManager.clearToken() }
                        auth.signOut()
                        navHostController.navigate("initial") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        // HU08: panel del administrador y gestión del catálogo turístico
        composable(route = "admin_dashboard") {
            AdminDashboardScreen(
                onManageDestinations = { navHostController.navigate("admin_destinations") },
                onManagePackages = { navHostController.navigate("admin_packages") },
                onManageProviders = { navHostController.navigate("admin_providers") },
                onNavigateBack = { navHostController.popBackStack() }
            )
        }

        composable(route = "admin_destinations") {
            AdminDestinationsScreen(
                onNavigateBack = { navHostController.popBackStack() },
                onCreateDestination = {
                    navHostController.navigate("admin_destination_form")
                },
                onEditDestination = { destinationId ->
                    navHostController.navigate(
                        "admin_destination_form?destinationId=${Uri.encode(destinationId)}"
                    )
                }
            )
        }

        // HU catálogo: proveedores turísticos del administrador
        composable(route = "admin_providers") {
            AdminProvidersScreen(
                onNavigateBack = { navHostController.popBackStack() }
            )
        }

        composable(
            route = "admin_destination_form?destinationId={destinationId}",
            arguments = listOf(
                navArgument("destinationId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            AdminDestinationFormScreen(
                destinationId = backStackEntry.arguments
                    ?.getString("destinationId")
                    .orEmpty(),
                onNavigateBack = { navHostController.popBackStack() },
                onSaved = { navHostController.popBackStack() }
            )
        }

        composable(route = "agent_home") {
            val fcmTokenManager: FCMTokenManager = koinInject()
            val coroutineScope = rememberCoroutineScope()
            AgentHomeScreen(
                auth = auth,
                onViewRequests = {
                    navHostController.navigate("agent_requests")
                },
                onViewPayments = {
                    navHostController.navigate("agent_payments")
                },
                onLogout = {
                    coroutineScope.launch {
                        withTimeoutOrNull(3_000) { fcmTokenManager.clearToken() }
                        auth.signOut()
                        navHostController.navigate("initial") {
                            popUpTo(0) {
                                inclusive = true
                            }
                        }
                    }
                }
            )
        }

        // HU07: panel del agente para revisar comprobantes de pago.
        composable(route = "agent_payments") {
            AgentPaymentsScreen(
                onNavigateBack = { navHostController.popBackStack() },
                onPaymentSelected = { paymentId ->
                    navHostController.navigate("agent_payment/$paymentId")
                }
            )
        }

        composable(route = "agent_payment/{paymentId}") { backStackEntry ->
            val paymentId = backStackEntry.arguments?.getString("paymentId").orEmpty()
            AgentPaymentDetailScreen(
                paymentId = paymentId,
                onNavigateBack = { navHostController.popBackStack() }
            )
        }

        composable(route = "home") {
            HomeScreen(
                auth = auth,
                navigateToTravelRequest = { destination ->
                    val route = if (destination == null) {
                        "travelRequest"
                    } else {
                        "travelRequest" +
                            "?destination=${Uri.encode(destination.location)}" +
                            "&packageTitle=${Uri.encode(destination.title)}" +
                            "&duration=${Uri.encode(destination.duration)}" +
                            "&description=${Uri.encode(destination.description.orEmpty())}"
                    }
                    navHostController.navigate(route)
                },
                navigateToQuotations = {
                    navHostController.navigate(route = "customer_quotations")
                },
                navigateToReservations = {
                    navHostController.navigate(route = "customer_reservations")
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

        composable(
            route = "travelRequest?destination={destination}" +
                "&packageTitle={packageTitle}&duration={duration}&description={description}",
            arguments = listOf(
                navArgument("destination") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("packageTitle") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("duration") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("description") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            TravelRequestScreen(
                suggestedDestination = backStackEntry.arguments
                    ?.getString("destination")
                    .orEmpty(),
                referencePackageTitle = backStackEntry.arguments
                    ?.getString("packageTitle")
                    .orEmpty(),
                referencePackageDuration = backStackEntry.arguments
                    ?.getString("duration")
                    .orEmpty(),
                referencePackageDescription = backStackEntry.arguments
                    ?.getString("description")
                    .orEmpty(),
                onBack = { navHostController.popBackStack() },
                onSaved = {
                    navHostController.navigate(route = "home") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = "agent_requests") {
            AgentRequestsScreen(
                onNavigateBack = {
                    navHostController.popBackStack()
                },
                onRequestSelected = { requestId ->
                    navHostController.navigate(
                        "create_quotation/$requestId"
                    )
                },
                onQuotationSelected = { quotationId ->
                    navHostController.navigate(
                        "agent_quotation_detail/$quotationId"
                    )
                }
            )
        }

        composable(route = "create_quotation/{requestId}") { backStackEntry ->
            val requestId =
                backStackEntry.arguments?.getString("requestId")
                    ?: ""

            val quotationViewModel: QuotationViewModel = koinViewModel()

            LaunchedEffect(requestId) {
                quotationViewModel.loadRequestForQuotation(requestId)
            }

            CreateQuotationScreen(
                viewModel = quotationViewModel,
                onNavigateBack = {
                    navHostController.popBackStack()
                },
                onQuotationCreated = { quotationId ->
                    navHostController.navigate(
                        "agent_quotation_detail/$quotationId"
                    ) {
                        popUpTo("agent_home") {
                            inclusive = false
                        }
                    }
                }
            )
        }

        composable(route = "agent_quotation_detail/{quotationId}") { backStackEntry ->
            val quotationId = backStackEntry.arguments?.getString("quotationId") ?: ""
            val quotationViewModel: QuotationViewModel = koinViewModel()
            val uiState by quotationViewModel.uiState.collectAsState()

            LaunchedEffect(quotationId) {
                quotationViewModel.loadQuotationById(quotationId)
            }

            val returnToAgentMenu = {
                navHostController.navigate("agent_home") {
                    popUpTo("agent_home") { inclusive = true }
                }
            }

            when (val state = uiState) {
                is QuotationUiState.Success -> {
                    QuotationDetailScreen(
                        quotation = state.quotation,
                        onNavigateBack = returnToAgentMenu,
                        showCustomerActions = false,
                        showAgentRevisionAction = true,
                        onAgentMenu = returnToAgentMenu,
                        onReviseQuotation = { id ->
                            navHostController.navigate("revise_quotation/$id")
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

        composable(route = "revise_quotation/{quotationId}") { backStackEntry ->
            val quotationId = backStackEntry.arguments?.getString("quotationId").orEmpty()
            val quotationViewModel: QuotationViewModel = koinViewModel()

            LaunchedEffect(quotationId) {
                quotationViewModel.loadQuotationForRevision(quotationId)
            }

            CreateQuotationScreen(
                viewModel = quotationViewModel,
                isRevision = true,
                onNavigateBack = { navHostController.popBackStack() },
                onQuotationCreated = { revisedQuotationId ->
                    navHostController.navigate(
                        "agent_quotation_detail/$revisedQuotationId"
                    ) {
                        popUpTo("agent_home") {
                            inclusive = false
                        }
                    }
                }
            )
        }

        composable(route = "customer_quotations") {
            CustomerQuotationsScreen(
                onNavigateBack = { navHostController.popBackStack() },
                onQuotationSelected = { quotationId ->
                    navHostController.navigate("customer_quotation/$quotationId")
                }
            )
        }

        composable(route = "customer_reservations") {
            CustomerReservationsScreen(
                onNavigateBack = { navHostController.popBackStack() },
                onReservationSelected = { reservationId ->
                    navHostController.navigate("customer_reservation/$reservationId")
                }
            )
        }

        composable(route = "customer_reservation/{reservationId}") { backStackEntry ->
            val reservationId = backStackEntry.arguments?.getString("reservationId").orEmpty()
            val reservationViewModel: CustomerReservationViewModel = koinViewModel()
            val detailState by reservationViewModel.detailState.collectAsState()

            LaunchedEffect(reservationId) {
                reservationViewModel.loadReservation(reservationId)
            }

            val reservation = detailState.reservation
            if (reservation != null) {
                ReservationDetailScreen(
                    reservation = reservation,
                    payment = detailState.payment,
                    onNavigateBack = { navHostController.popBackStack() },
                    onNavigateToPayment = {
                        navHostController.navigate(
                            "payment/${reservation.quotationId}/${reservation.id}/${reservation.totalAmount}/${reservation.currency}"
                        )
                    }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (detailState.isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Text(
                            text = detailState.errorMessage
                                ?: "No se pudo mostrar la reserva."
                        )
                    }
                }
            }
        }

        composable(route = "customer_quotation/{quotationId}") { backStackEntry ->
            val quotationId = backStackEntry.arguments?.getString("quotationId").orEmpty()
            val customerViewModel: CustomerQuotationViewModel = koinViewModel()
            val detailState by customerViewModel.detailState.collectAsState()
            val responseState by customerViewModel.responseState.collectAsState()

            LaunchedEffect(quotationId) {
                customerViewModel.loadQuotation(quotationId)
            }

            val quotation = detailState.quotation
            if (quotation != null) {
                QuotationDetailScreen(
                    quotation = quotation,
                    observations = detailState.observations,
                    showCustomerActions = true,
                    isSavingResponse = responseState is QuotationResponseUiState.Saving,
                    responseMessage = when (val response = responseState) {
                        is QuotationResponseUiState.Success -> response.message
                        is QuotationResponseUiState.Error -> response.message
                        else -> null
                    },
                    responseIsError = responseState is QuotationResponseUiState.Error,
                    onNavigateBack = { navHostController.popBackStack() },
                    onAcceptQuotation = customerViewModel::acceptQuotation,
                    onObserveQuotation = customerViewModel::observeQuotation,
                    onCancelQuotation = customerViewModel::cancelQuotation,
                    onViewReservation = { reservationId ->
                        navHostController.navigate("customer_reservation/$reservationId")
                    }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (detailState.isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Text(
                            text = detailState.errorMessage
                                ?: "No se pudo mostrar la cotización."
                        )
                    }
                }
            }
        }

        // HU07: Ruta del Registro de Pago con CameraX y WorkManager
        composable(route = "payment/{quotationId}/{reservationId}/{amount}/{currency}") { backStackEntry ->
            val quotationId = backStackEntry.arguments?.getString("quotationId").orEmpty()
            val reservationId = backStackEntry.arguments?.getString("reservationId").orEmpty()
            val amount = backStackEntry.arguments?.getString("amount")?.toDoubleOrNull() ?: 0.0
            val currency = backStackEntry.arguments?.getString("currency") ?: "PEN"

            PaymentScreen(
                quotationId = quotationId,
                reservationId = reservationId,
                totalAmount = amount,
                currency = currency,
                onNavigateBack = { navHostController.popBackStack() },
                onPaymentSuccess = {
                    navHostController.navigate("customer_reservations") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }
    }
}
