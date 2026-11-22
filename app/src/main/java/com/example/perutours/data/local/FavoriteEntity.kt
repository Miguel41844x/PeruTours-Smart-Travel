package com.example.perutours.data.local

import androidx.room.Entity
import androidx.room.Index
import com.example.perutours.data.model.FavoriteItem
import com.example.perutours.data.model.FavoriteType

@Entity(tableName = "favorites", primaryKeys = ["userId", "itemId", "kind"],
    indices = [Index("userId")])
data class FavoriteEntity(
    val userId: String,
    val itemId: String,
    val kind: String,
    val destinationId: String,
    val title: String,
    val location: String,
    val description: String,
    val imageUrl: String,
    val category: String,
    val duration: String,
    val priceLabel: String,
    val savedAtMillis: Long
) {
    fun toModel() = FavoriteItem(itemId, FavoriteType.valueOf(kind), destinationId, title,
        location, description, imageUrl, category, duration, priceLabel, savedAtMillis)
}

fun FavoriteItem.toEntity(userId: String) = FavoriteEntity(userId, id, type.name,
    destinationId, title, location, description, imageUrl, category, duration, priceLabel, savedAtMillis)
