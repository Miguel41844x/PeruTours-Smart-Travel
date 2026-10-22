package com.example.perutours.di

import com.example.perutours.data.messaging.FCMTokenManager
import com.example.perutours.data.location.LocationDataSource
import com.example.perutours.data.location.LocationRepository
import com.example.perutours.data.repository.AuthRepository
import com.example.perutours.data.repository.AdminDashboardRepository
import com.example.perutours.data.repository.CatalogImageRepository
import com.example.perutours.data.repository.DestinationCatalogRepository
import com.example.perutours.data.repository.PackageRepository
import com.example.perutours.data.repository.ProfileRepository
import com.example.perutours.data.repository.QuotationRepository
import com.example.perutours.data.repository.ReservationDataSource
import com.example.perutours.data.repository.ReservationRepository
import com.example.perutours.data.repository.ServiceCatalogRepository
import com.example.perutours.data.repository.TourProviderRepository
import com.example.perutours.data.repository.TravelRequestRepository
import com.example.perutours.data.repository.TravelRequestDataSource
import com.example.perutours.data.repository.PaymentRepository
import com.example.perutours.data.repository.PaymentReviewDataSource
import com.example.perutours.data.repository.PaymentReviewRepository
import com.example.perutours.data.repository.SalesDashboardDataSource
import com.example.perutours.data.repository.SalesDashboardRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

import com.example.perutours.ui.screens.login.LoginViewModel
import com.example.perutours.ui.screens.signup.SignUpViewModel
import com.example.perutours.ui.screens.profile.ProfileViewModel
import com.example.perutours.ui.screens.admin.AdminDashboardViewModel
import com.example.perutours.ui.screens.admin.destination.AdminDestinationFormViewModel
import com.example.perutours.ui.screens.admin.destination.AdminDestinationsViewModel
import com.example.perutours.ui.screens.admin.packages.AdminPackagesViewModel
import com.example.perutours.ui.screens.admin.provider.AdminProvidersViewModel
import com.example.perutours.ui.screens.agent.AgentRequestsViewModel
import com.example.perutours.ui.screens.agent.AgentHomeViewModel
import com.example.perutours.ui.screens.agent.AgentPaymentViewModel
import com.example.perutours.ui.screens.quotation.QuotationViewModel
import com.example.perutours.ui.screens.quotation.CustomerQuotationViewModel
import com.example.perutours.ui.screens.reservation.CustomerReservationViewModel
import com.example.perutours.ui.screens.payment.PaymentViewModel
import com.example.perutours.ui.screens.sales.SalesDashboardViewModel

import android.app.Application
import androidx.work.WorkManager
import com.example.perutours.data.local.AppDatabase
import com.example.perutours.data.remote.NetworkClient
import com.example.perutours.data.repository.DestinationRepository
import com.example.perutours.ui.screens.home.HomeViewModel
import com.example.perutours.ui.screens.request.TravelRequestPlaceResolver
import com.example.perutours.ui.screens.request.TravelRequestViewModel
import com.example.perutours.domain.analytics.SalesAnalyticsCalculator
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext

val appModule = module {

    // 1. Servicios de Firebase compartidos (Singletons)
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single { FirebaseStorage.getInstance() }

    // 2. Repositorios de datos
    single {
        AuthRepository(
            auth = get(),
            firestore = get()
        )
    }

    single {
        ProfileRepository(
            auth = get(),
            firestore = get(),
            storage = get(),
            context = androidContext()
        )
    }

    single<TravelRequestDataSource> {
        TravelRequestRepository(
            auth = get(),
            firestore = get()
        )
    }

    single<LocationDataSource> {
        LocationRepository(androidContext())
    }

    single {
        QuotationRepository(
            auth = get(),
            firestore = get()
        )
    }

    single<ReservationDataSource> {
        ReservationRepository(
            auth = get(),
            firestore = get()
        )
    }

    single {
        ServiceCatalogRepository(
            firestore = get()
        )
    }

    // HU catálogo turístico: destinos, paquetes y proveedores en Firestore
    single {
        DestinationCatalogRepository(
            firestore = get(),
            auth = get()
        )
    }

    single {
        AdminDashboardRepository(
            firestore = get()
        )
    }

    single {
        PackageRepository(
            firestore = get(),
            auth = get()
        )
    }

    single {
        TourProviderRepository(
            firestore = get(),
            auth = get()
        )
    }

    // HU catálogo: imágenes de portada en Firebase Storage
    single {
        CatalogImageRepository(
            storage = get(),
            auth = get()
        )
    }

    single {
        FCMTokenManager(
            auth = get(),
            firestore = get()
        )
    }

    // HU07: Repositorio de Pagos y WorkManager
    single {
        WorkManager.getInstance(androidContext())
    }

    single {
        PaymentRepository(
            context = androidContext(),
            workManager = get()
        )
    }

    // HU07: revisión del pago (lectura del cliente y panel del agente)
    single<PaymentReviewDataSource> {
        PaymentReviewRepository(
            auth = get(),
            firestore = get()
        )
    }

    // 3. Room SQLite
    single {
        AppDatabase.getInstance(androidContext())
    }

    single {
        get<AppDatabase>().destinationDao()
    }

    // 4. Retrofit API
    single {
        NetworkClient.getApiService(androidContext())
    }

    // Repositorio de destinos
    single {
        DestinationRepository(
            apiService = get(),
            destinationDao = get()
        )
    }

    // HU09: analítica comercial calculada desde Firestore.
    single { SalesAnalyticsCalculator() }

    single<SalesDashboardDataSource> {
        SalesDashboardRepository(
            firestore = get(),
            calculator = get()
        )
    }

    // 5. ViewModels Inyectados con Koin
    viewModel {
        LoginViewModel(
            authRepository = get(),
            fcmTokenManager = get()
        )
    }

    viewModel {
        SignUpViewModel(
            authRepository = get()
        )
    }

    viewModel {
        ProfileViewModel(
            repository = get(),
            auth = get()
        )
    }

    viewModel {
        AgentRequestsViewModel(
            repository = get(),
            quotationRepository = get()
        )
    }

    viewModel {
        AgentHomeViewModel(
            auth = get(),
            firestore = get()
        )
    }

    viewModel {
        QuotationViewModel(
            repository = get(),
            catalogRepository = get(),
            travelRequestRepository = get(),
            profileRepository = get()
        )
    }

    viewModel {
        CustomerQuotationViewModel(
            repository = get()
        )
    }

    viewModel {
        CustomerReservationViewModel(
            repository = get(),
            paymentRepository = get()
        )
    }

    viewModel {
        HomeViewModel(
            application = androidApplication(),
            repository = get(),
            profileRepository = get(),
            auth = get()
        )
    }

    factory {
        TravelRequestPlaceResolver(
            locationDataSource = get()
        )
    }

    viewModel {
        TravelRequestViewModel(
            application = androidApplication(),
            requestRepository = get(),
            locationRepository = get(),
            placeResolver = get()
        )
    }

    // HU07: ViewModel de Pago del Viaje
    viewModel {
        PaymentViewModel(
            paymentRepository = get(),
            reservationRepository = get()
        )
    }

    // HU07: revisión de comprobantes por el agente
    viewModel {
        AgentPaymentViewModel(
            repository = get()
        )
    }

    viewModel {
        SalesDashboardViewModel(
            repository = get()
        )
    }

    // HU08 / catálogo turístico: panel y pantallas del administrador
    viewModel {
        AdminDashboardViewModel(
            repository = get()
        )
    }

    viewModel {
        AdminDestinationsViewModel(
            repository = get()
        )
    }

    viewModel {
        AdminDestinationFormViewModel(
            repository = get(),
            imageRepository = get(),
            auth = get()
        )
    }

    viewModel {
        AdminProvidersViewModel(
            repository = get()
        )
    }

    viewModel {
        AdminPackagesViewModel(
            packageRepository = get(),
            destinationRepository = get(),
            serviceCatalogRepository = get(),
            auth = get()
        )
    }
}
