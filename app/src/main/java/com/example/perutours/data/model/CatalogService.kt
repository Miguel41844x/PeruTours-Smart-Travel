package com.example.perutours.data.model

data class CatalogService(
    val id: String = "",
    val name: String = "",
    val city: String = "",
    val category: String = "",
    val unitCost: Double = 0.0,
    val description: String = "",
    val defaultQuantity: Int = 1,
    val active: Boolean = true
)
