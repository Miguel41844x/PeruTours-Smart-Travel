package com.example.perutours.data.model

data class AdminMetrics(
    val totalProviders: Int = 0,
    val activeProviders: Int = 0,
    val totalServices: Int = 0,
    val availableServices: Int = 0,
    val totalDestinations: Int = 0,
    val publishedDestinations: Int = 0,
    val totalPackages: Int = 0,
    val publishedPackages: Int = 0,
    val packagesByStatus: Map<String, Int> = emptyMap(),
    val servicesByDestination: Map<String, Int> = emptyMap(),
    val usersByRole: Map<String, Int> = emptyMap(),
    val alerts: List<CatalogAlert> = emptyList(),
    val isCached: Boolean = false,
    val updatedAtMillis: Long = 0L
) {
    val totalUsers: Int get() = usersByRole.values.sum()
    val hasData: Boolean get() = updatedAtMillis > 0L
}

enum class CatalogArea { PROVIDERS, SERVICES, DESTINATIONS, PACKAGES }

data class CatalogAlert(val area: CatalogArea, val title: String, val count: Int)
