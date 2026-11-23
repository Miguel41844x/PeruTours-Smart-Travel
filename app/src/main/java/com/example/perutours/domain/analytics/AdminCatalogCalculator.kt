package com.example.perutours.domain.analytics

import com.example.perutours.data.model.AdminMetrics
import com.example.perutours.data.model.CatalogAlert
import com.example.perutours.data.model.CatalogArea
import com.example.perutours.data.model.CatalogService
import com.example.perutours.data.model.CatalogStatus
import com.example.perutours.data.model.TourDestination
import com.example.perutours.data.model.TourPackage
import com.example.perutours.data.model.TourProvider
import com.example.perutours.data.model.UserRole

/** Indicadores de mantenimiento del catálogo; no mezcla cobros con operación. */
class AdminCatalogCalculator {
    fun calculate(
        providers: List<TourProvider>, services: List<CatalogService>,
        destinations: List<TourDestination>, packages: List<TourPackage>, roles: List<String>,
        nowMillis: Long = System.currentTimeMillis()
    ): AdminMetrics {
        val providersById = providers.associateBy { it.id }
        val destinationsById = destinations.associateBy { it.id }
        val available = services.filter {
            it.active && providersById[it.providerId]?.active == true &&
                destinationsById[it.destinationId]?.published == true &&
                it.unitCost.isFinite() && it.unitCost > 0
        }
        val availableById = available.associateBy { it.id }
        val activeServicesByProvider = services.filter { it.active }.groupBy { it.providerId }
        val alerts = buildList {
            fun addIfAny(area: CatalogArea, title: String, count: Int) {
                if (count > 0) add(CatalogAlert(area, title, count))
            }
            addIfAny(CatalogArea.PROVIDERS, "Proveedores activos sin servicios",
                providers.count { it.active && activeServicesByProvider[it.id].isNullOrEmpty() })
            addIfAny(CatalogArea.SERVICES, "Servicios activos no disponibles para cotizar",
                services.count { it.active && it.id !in availableById })
            addIfAny(CatalogArea.PACKAGES, "Paquetes publicados por revisar", packages.count { pack ->
                pack.published && (destinationsById[pack.destinationId]?.published != true ||
                    pack.title.isBlank() || pack.coverImageUrl.isBlank() ||
                    !pack.priceAmount.isFinite() || pack.priceAmount <= 0 ||
                    pack.includedServiceIds.isEmpty() || pack.includedServiceIds.any { serviceId ->
                        availableById[serviceId]?.destinationId != pack.destinationId
                    })
            })
            addIfAny(CatalogArea.DESTINATIONS, "Destinos pendientes de publicación",
                destinations.count { CatalogStatus.from(it.status) == CatalogStatus.BORRADOR })
            addIfAny(CatalogArea.PACKAGES, "Paquetes en borrador",
                packages.count { CatalogStatus.from(it.status) == CatalogStatus.BORRADOR })
        }
        return AdminMetrics(
            totalProviders = providers.size, activeProviders = providers.count { it.active },
            totalServices = services.size, availableServices = available.size,
            totalDestinations = destinations.size, publishedDestinations = destinations.count { it.published },
            totalPackages = packages.size, publishedPackages = packages.count { it.published },
            packagesByStatus = CatalogStatus.entries.associate { status ->
                status.name to packages.count { CatalogStatus.from(it.status) == status }
            },
            servicesByDestination = available.groupingBy {
                destinationsById.getValue(it.destinationId).name
            }.eachCount().toList().sortedWith(compareByDescending<Pair<String, Int>> { it.second }
                .thenBy { it.first }).take(6).toMap(),
            usersByRole = roles.groupingBy { UserRole.fromWire(it).name }.eachCount(),
            alerts = alerts, updatedAtMillis = nowMillis
        )
    }
}
