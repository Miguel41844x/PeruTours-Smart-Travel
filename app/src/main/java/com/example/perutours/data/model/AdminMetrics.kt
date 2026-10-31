package com.example.perutours.data.model

data class AdminMetrics(
    val totalRevenue: Double = 0.0,
    val totalReservations: Int = 0,
    val activeCustomers: Int = 0,
    val statusSummary: Map<String, Int> = emptyMap(),
    val monthlyRevenue: Map<String, Double> = emptyMap(),
    val isCached: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)
