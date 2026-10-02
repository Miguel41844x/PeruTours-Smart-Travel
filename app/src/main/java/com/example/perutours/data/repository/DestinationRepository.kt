package com.example.perutours.data.repository

import com.example.perutours.data.local.DestinationDao
import com.example.perutours.data.local.DestinationEntity
import com.example.perutours.data.local.toEntity
import com.example.perutours.data.remote.DestinationsApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DestinationRepository(
    private val apiService: DestinationsApiService,
    private val destinationDao: DestinationDao
) {
    // Escucha la base de datos local Room (Single Source of Truth)
    val localDestinations: Flow<List<DestinationEntity>> = destinationDao.getAllDestinations()

    // Sincroniza la API con Room SQLite en segundo plano (Dispatchers.IO)
    suspend fun refreshDestinations(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val remoteDestinations = apiService.getDestinations()
            val entities = remoteDestinations.map { it.toEntity() }
            destinationDao.insertAll(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}