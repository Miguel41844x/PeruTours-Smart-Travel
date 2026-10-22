package com.example.perutours.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Punto de interés cercano a un destino, obtenido de la API externa
 * de Geoapify (Places) y cacheado localmente.
 *
 * Room evita repetir la consulta a la API mientras la caché siga vigente
 * y permite mostrar los lugares sin conexión.
 */
@Entity(
    tableName = "places",
    indices = [Index("destinationId")]
)
data class PlaceEntity(
    @PrimaryKey val id: String,
    val destinationId: String,
    val name: String,
    val categories: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double,
    val fetchedAtMillis: Long
)
