package com.example.perutours.data.remote

import com.google.gson.annotations.SerializedName

data class DestinationDto(
    @SerializedName("id")
    val id: String,

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
    val description: String? = null
)
