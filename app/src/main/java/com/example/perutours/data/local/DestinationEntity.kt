package com.example.perutours.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.perutours.data.remote.DestinationDto

@Entity(tableName = "destinations")
data class DestinationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val location: String,
    val durationText: String,
    val durationDays: Int,
    val priceAmount: Double,
    val ratingScore: Double,
    val category: String = "Popular",
    val imageUrl: String,
    val description: String? = null,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun toDto(): DestinationDto {
        return DestinationDto(
            id = id,
            title = title,
            location = location,
            duration = durationText,
            price = "S/ ${priceAmount.toInt()}",
            rating = ratingScore.toString(),
            category = category,
            imageUrl = imageUrl,
            description = description
        )
    }
}

fun DestinationDto.toEntity(timestamp: Long = System.currentTimeMillis()): DestinationEntity {
    val cleanPrice = price.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
    val cleanRating = rating.toDoubleOrNull() ?: 0.0
    val daysCount = duration.filter { it.isDigit() }.take(1).toIntOrNull() ?: 1

    return DestinationEntity(
        id = id,
        title = title,
        location = location,
        durationText = duration,
        durationDays = daysCount,
        priceAmount = cleanPrice,
        ratingScore = cleanRating,
        category = category,
        imageUrl = imageUrl,
        description = description,
        updatedAtMillis = timestamp
    )
}