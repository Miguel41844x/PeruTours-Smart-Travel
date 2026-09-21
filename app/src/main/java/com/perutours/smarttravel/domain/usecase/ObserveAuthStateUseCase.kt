package com.perutours.smarttravel.domain.usecase

import com.perutours.smarttravel.domain.model.AuthUser
import com.perutours.smarttravel.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAuthStateUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke(): Flow<AuthUser?> = repository.authStateFlow
}
