package com.example.perutours.data.remote

import com.google.gson.annotations.SerializedName

/**
 * Modelo de presentación que consume la UI del cliente.
 *
 * Originalmente describía el JSON alojado en GitHub; hoy es el resultado de
 * mapear los paquetes PUBLICADOS de Firestore (ver DestinationEntity.toDto).
 */
data class DestinationDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("destinationId")
    val destinationId: String = "",

    @SerializedName("title")
    val title: String,

    @SerializedName("location")
    val location: String,

    @SerializedName("duration")
    val duration: String,

    @SerializedName("price")
    val price: String,

    @SerializedName("rating")
    val rating: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("imageUrl")
    val imageUrl: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("summary")
    val summary: String = "",

    @SerializedName("latitude")
    val latitude: Double = 0.0,

    @SerializedName("longitude")
    val longitude: Double = 0.0
)
