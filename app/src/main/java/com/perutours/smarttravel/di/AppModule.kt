package com.perutours.smarttravel.di

import com.perutours.smarttravel.data.auth.FakeAuthRepository
import com.perutours.smarttravel.domain.repository.AuthRepository
import com.perutours.smarttravel.domain.usecase.LoginUseCase
import com.perutours.smarttravel.domain.usecase.LogoutUseCase
import com.perutours.smarttravel.domain.usecase.ObserveAuthStateUseCase
import com.perutours.smarttravel.domain.usecase.RegisterUseCase
import com.perutours.smarttravel.domain.usecase.ReloadUserUseCase
import com.perutours.smarttravel.domain.usecase.SendEmailVerificationUseCase

object AppModule {

    private val authRepository: AuthRepository by lazy {
        FakeAuthRepository()
    }

    val loginUseCase: LoginUseCase by lazy { LoginUseCase(authRepository) }
    val registerUseCase: RegisterUseCase by lazy { RegisterUseCase(authRepository) }
    val observeAuthStateUseCase: ObserveAuthStateUseCase by lazy { ObserveAuthStateUseCase(authRepository) }
    val logoutUseCase: LogoutUseCase by lazy { LogoutUseCase(authRepository) }
    val sendEmailVerificationUseCase: SendEmailVerificationUseCase by lazy { SendEmailVerificationUseCase(authRepository) }
    val reloadUserUseCase: ReloadUserUseCase by lazy { ReloadUserUseCase(authRepository) }
}
