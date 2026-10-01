package com.example.perutours.data.repository

import com.example.perutours.data.local.DestinationDao
import com.example.perutours.data.local.DestinationEntity
import com.example.perutours.data.local.toEntity
import com.example.perutours.data.remote.DestinationsApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DestinationRepository(
    private val apiService: DestinationsApiService,
    private val destinationDao: DestinationDao
) {
    // Escucha la base de datos local Room
    val localDestinations: Flow<List<DestinationEntity>> = destinationDao.getAllDestinations()

    // Sincroniza la API con Room SQLite
    suspend fun refreshDestinations(): Result<Unit> {
        return try {
            val remoteDestinations = apiService.getDestinations()
            val entities = remoteDestinations.map { it.toEntity() }
            destinationDao.insertAll(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}