package com.example.perutours.data.model

import com.example.perutours.data.remote.DestinationDto

enum class FavoriteType { DESTINATION, PACKAGE }

/** Información consultable sin red; una solicitud vuelve a validar el catálogo vigente. */
data class FavoriteItem(
    val id: String,
    val type: FavoriteType,
    val destinationId: String,
    val title: String,
    val location: String,
    val description: String,
    val imageUrl: String,
    val category: String = "",
    val duration: String = "",
    val priceLabel: String = "",
    val savedAtMillis: Long = System.currentTimeMillis()
) {
    val key: String get() = type.name + ":" + id

    companion object {
        fun destination(value: TourDestination) = FavoriteItem(
            id = value.id, type = FavoriteType.DESTINATION, destinationId = value.id,
            title = value.name, location = value.department, description = value.summary,
            imageUrl = value.coverImageUrl, category = value.categories.joinToString(" · ")
        )

        fun travel(value: DestinationDto) = FavoriteItem(
            id = value.id, type = FavoriteType.PACKAGE, destinationId = value.destinationId,
            title = value.title, location = value.location,
            description = value.description?.takeIf(String::isNotBlank) ?: value.summary,
            imageUrl = value.imageUrl, category = value.category,
            duration = value.duration, priceLabel = value.price
        )

        fun travel(value: TourPackage, location: String) = FavoriteItem(
            id = value.id, type = FavoriteType.PACKAGE, destinationId = value.destinationId,
            title = value.title, location = location, description = value.summary,
            imageUrl = value.coverImageUrl, category = value.category,
            duration = value.durationLabel(), priceLabel = value.priceLabel()
        )
    }
}
