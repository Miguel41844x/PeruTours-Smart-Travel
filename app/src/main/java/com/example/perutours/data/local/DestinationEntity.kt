package com.example.perutours.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.perutours.data.remote.DestinationDto

@Entity(tableName = "destinations")
data class DestinationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val location: String,
    val price: String,
    val rating: String,
    val duration: String,
    val imageUrl: String,
    val description: String? = null
) {
    fun toDto(): DestinationDto {
        return DestinationDto(
            id = id,
            title = title,
            location = location,
            price = price,
            rating = rating,
            duration = duration,
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
        price = price,
        rating = rating,
        duration = duration,
        imageUrl = imageUrl,
        description = description
    )
}