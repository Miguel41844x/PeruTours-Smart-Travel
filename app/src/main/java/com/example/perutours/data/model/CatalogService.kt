package com.example.perutours.data.model

data class CatalogService(
    val id: String = "",
    val name: String = "",
    val destinationId: String = "",
    val providerId: String = "",
    val city: String = "",
    val category: String = "",
    val unitCost: Double = 0.0,
    val commercialCost: Double = 0.0,
    val supplierName: String = "",
    val description: String = "",
    val defaultQuantity: Int = 1,
    val durationMinutes: Int = 60,
    val requiresScheduling: Boolean = true,
    val imageUrl: String = "",
    val active: Boolean = true,
    val requestable: Boolean = false
)
