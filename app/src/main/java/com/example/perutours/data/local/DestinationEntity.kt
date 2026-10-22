package com.example.perutours.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.perutours.data.model.TourDestination
import com.example.perutours.data.model.TourPackage

/**
 * Caché local de los paquetes publicados que ve el cliente.
 *
 * Room sigue siendo la fuente única de origen de la UI: Firestore se
 * consulta solo cuando expira la TTL y los resultados se guardan aquí para
 * soportar modo offline.
 */
@Entity(tableName = "destinations")
data class DestinationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(defaultValue = "")
    val destinationId: String = "",
    val title: String,
    val location: String,
    val durationText: String,
    val durationDays: Int,
    val priceAmount: Double,
    val ratingScore: Double,
    val category: String = "Popular",
    val imageUrl: String,
    val description: String? = null,
    @ColumnInfo(defaultValue = "")
    val summary: String = "",
    @ColumnInfo(defaultValue = "0.0")
    val latitude: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0")
    val longitude: Double = 0.0,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun toDto(): com.example.perutours.data.remote.DestinationDto {
        return com.example.perutours.data.remote.DestinationDto(
            id = id,
            destinationId = destinationId,
            title = title,
            location = location,
            duration = durationText,
            price = "S/ ${priceAmount.toInt()}",
            rating = ratingScore.toString(),
            category = category,
            imageUrl = imageUrl,
            description = description,
            summary = summary,
            latitude = latitude,
            longitude = longitude
        )
    }
}

/**
 * Convierte un paquete PUBLICADO de Firestore en la fila local que la
 * pantalla del cliente entiende. El destino aporta el lugar y las
 * coordenadas que después alimentan los puntos de interés cercanos.
 */
fun TourPackage.toEntity(destination: TourDestination?): DestinationEntity {
    val place = destination?.department?.takeIf { it.isNotBlank() } ?: "Perú"
    return DestinationEntity(
        id = id,
        destinationId = destinationId,
        title = title,
        location = "$place, Perú",
        durationText = durationLabel(),
        durationDays = durationDays,
        priceAmount = priceAmount,
        ratingScore = ratingScore,
        category = category,
        imageUrl = coverImageUrl,
        description = summary,
        summary = destination?.summary.orEmpty(),
        latitude = destination?.latitude ?: 0.0,
        longitude = destination?.longitude ?: 0.0,
        updatedAtMillis = updatedAtMillis
    )
}
