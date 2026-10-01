package com.example.perutours.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.perutours.data.remote.DestinationDto

@Entity(tableName = "destinations")
data class DestinationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val location: String,
    val duration: String,
    val price: String,
    val rating: String,
    val category: String = "Popular",
    val imageUrl: String,
    val description: String? = null
) {
    fun toDto(): DestinationDto {
        return DestinationDto(
            id = id,
            title = title,
            location = location,
            duration = duration,
            price = price,
            rating = rating,
            category = category,
            imageUrl = imageUrl,
            description = description
        )
    }
}

fun DestinationDto.toEntity(): DestinationEntity {
    return DestinationEntity(
        id = id,
        title = title,
        location = location,
        duration = duration,
        price = price,
        rating = rating,
        category = category,
        imageUrl = imageUrl,
        description = description
    )
}