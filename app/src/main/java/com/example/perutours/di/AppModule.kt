package com.example.perutours.di

import com.example.perutours.data.messaging.FCMTokenManager
import com.example.perutours.data.location.LocationDataSource
import com.example.perutours.data.location.LocationRepository
import com.example.perutours.data.repository.AuthRepository
import com.example.perutours.data.repository.ProfileRepository
import com.example.perutours.data.repository.QuotationRepository
import com.example.perutours.data.repository.ReservationDataSource
import com.example.perutours.data.repository.ReservationRepository
import com.example.perutours.data.repository.ServiceCatalogRepository
import com.example.perutours.data.repository.TravelRequestRepository
import com.example.perutours.data.repository.TravelRequestDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

import com.example.perutours.ui.screens.login.LoginViewModel
import com.example.perutours.ui.screens.signup.SignUpViewModel
import com.example.perutours.ui.screens.profile.ProfileViewModel
import com.example.perutours.ui.screens.agent.AgentRequestsViewModel
import com.example.perutours.ui.screens.agent.AgentHomeViewModel
import com.example.perutours.ui.screens.quotation.QuotationViewModel
import com.example.perutours.ui.screens.quotation.CustomerQuotationViewModel
import com.example.perutours.ui.screens.reservation.CustomerReservationViewModel

import android.app.Application
import com.example.perutours.data.local.AppDatabase
import com.example.perutours.data.remote.NetworkClient
import com.example.perutours.data.repository.DestinationRepository
import com.example.perutours.ui.screens.home.HomeViewModel
import com.example.perutours.ui.screens.request.TravelRequestViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext

val appModule = module {

    // Servicios de Firebase compartidos
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single { FirebaseStorage.getInstance() }

    // Repositorios
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

    single {
        FCMTokenManager(
            auth = get(),
            firestore = get()
        )
    }

    // Room
    single {
        AppDatabase.getInstance(androidContext())
    }

    single {
        get<AppDatabase>().destinationDao()
    }

    // Retrofit
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

    // ViewModels
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
            repository = get()
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

    viewModel {
        TravelRequestViewModel(
            application = androidApplication(),
            requestRepository = get(),
            locationRepository = get()
        )
    }
}
