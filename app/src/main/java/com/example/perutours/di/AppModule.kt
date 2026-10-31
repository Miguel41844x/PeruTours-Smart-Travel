package com.example.perutours.di

import com.example.perutours.data.messaging.FCMTokenManager
import com.example.perutours.data.location.LocationDataSource
import com.example.perutours.data.location.LocationRepository
import com.example.perutours.data.repository.AuthRepository
import com.example.perutours.data.repository.AdminDashboardRepository
import com.example.perutours.data.repository.AgentTripDataSource
import com.example.perutours.data.repository.AgentTripRepository
import com.example.perutours.data.repository.CatalogImageRepository
import com.example.perutours.data.repository.DestinationCatalogRepository
import com.example.perutours.data.repository.GeocodingRepository
import com.example.perutours.data.repository.PackageRepository
import com.example.perutours.data.repository.PointsOfInterestRepository
import com.example.perutours.data.repository.ProfileRepository
import com.example.perutours.data.repository.QuotationRepository
import com.example.perutours.data.repository.ReservationDataSource
import com.example.perutours.data.repository.ReservationRepository
import com.example.perutours.data.repository.ServiceCatalogRepository
import com.example.perutours.data.repository.TourProviderRepository
import com.example.perutours.data.repository.TravelRequestRepository
import com.example.perutours.data.repository.TravelRequestDataSource
import com.example.perutours.data.repository.FirestoreTripRemoteDataSource
import com.example.perutours.data.repository.TripDataSource
import com.example.perutours.data.repository.TripRemoteDataSource
import com.example.perutours.data.repository.TripRepository
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
import com.example.perutours.ui.screens.destination.DestinationDetailViewModel
import com.example.perutours.ui.screens.agent.AgentRequestsViewModel
import com.example.perutours.ui.screens.agent.AgentHomeViewModel
import com.example.perutours.ui.screens.agent.AgentPaymentViewModel
import com.example.perutours.ui.screens.quotation.QuotationViewModel
import com.example.perutours.ui.screens.quotation.CustomerQuotationViewModel
import com.example.perutours.ui.screens.reservation.CustomerReservationViewModel
import com.example.perutours.ui.screens.payment.PaymentViewModel
import com.example.perutours.ui.screens.sales.SalesDashboardViewModel
import com.example.perutours.ui.screens.trip.TripListViewModel
import com.example.perutours.ui.screens.trip.TripDetailViewModel
import com.example.perutours.ui.screens.trip.AgentTripViewModel

import android.app.Application
import androidx.work.WorkManager
import com.example.perutours.data.local.AppDatabase
import com.example.perutours.data.remote.geoapify.GeoapifyClient
import com.example.perutours.data.repository.DestinationRepository
import com.example.perutours.ui.screens.home.HomeViewModel
import com.example.perutours.ui.screens.request.TravelRequestPlaceResolver
import com.example.perutours.ui.screens.request.TravelRequestViewModel
import com.example.perutours.domain.analytics.SalesAnalyticsCalculator
import com.example.perutours.service.TripBackgroundScheduler
import com.example.perutours.service.TripWorkScheduler
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
            firestore = get(),
            adminCacheDao = get()
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

    // HU catálogo: API externa de Geoapify (geocodificación y lugares)
    single {
        GeoapifyClient.getApiService(androidContext().cacheDir)
    }

    single {
        GeocodingRepository(api = get())
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

    single<TripBackgroundScheduler> {
        TripWorkScheduler(
            workManager = get()
        )
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

    single {
        get<AppDatabase>().placeDao()
    }

    single {
        get<AppDatabase>().tripDao()
    }

    single {
        get<AppDatabase>().adminCacheDao()
    }

    // Centro de viaje: Firestore como origen y Room como lectura offline.
    single<TripRemoteDataSource> {
        FirestoreTripRemoteDataSource(
            auth = get(),
            firestore = get()
        )
    }

    single<TripDataSource> {
        TripRepository(
            remote = get(),
            tripDao = get(),
            scheduler = get()
        )
    }

    single<AgentTripDataSource> {
        AgentTripRepository(
            auth = get(),
            firestore = get()
        )
    }

    // HU catálogo: puntos de interés cercanos (Geoapify + caché Room)
    single {
        PointsOfInterestRepository(
            api = get(),
            placeDao = get()
        )
    }

    // Repositorio de catálogo del cliente (Firestore -> Room)
    single {
        DestinationRepository(
            packageRepository = get(),
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

    viewModel {
        TripListViewModel(repository = get())
    }

    viewModel {
        TripDetailViewModel(
            repository = get(),
            locationRepository = get()
        )
    }

    viewModel {
        AgentTripViewModel(repository = get())
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
            geocodingRepository = get(),
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

    // HU catálogo: detalle del destino con puntos de interés cercanos
    viewModel {
        DestinationDetailViewModel(
            destinationRepository = get(),
            packageRepository = get(),
            pointsOfInterestRepository = get()
        )
    }
}
