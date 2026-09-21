package com.perutours.smarttravel.domain.usecase

import com.perutours.smarttravel.domain.model.AuthUser
import com.perutours.smarttravel.domain.repository.AuthRepository
import javax.inject.Inject

class ReloadUserUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): AuthUser? {
        return repository.reloadUser()
    }
}
