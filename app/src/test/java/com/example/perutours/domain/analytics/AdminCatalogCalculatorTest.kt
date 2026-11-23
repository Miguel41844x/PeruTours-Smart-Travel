package com.example.perutours.domain.analytics

import com.example.perutours.data.model.*
import org.junit.Assert.*
import org.junit.Test

class AdminCatalogCalculatorTest {
    private val calculator = AdminCatalogCalculator()
    private val provider = TourProvider(id = "hotel", active = true)
    private val destination = TourDestination(id = "cusco", name = "Cusco", status = "PUBLICADO")
    private val service = CatalogService(id = "night", providerId = "hotel", destinationId = "cusco", unitCost = 80.0)
    private val pack = TourPackage(id = "pack", title = "Cusco cultural", destinationId = "cusco",
        priceAmount = 200.0, coverImageUrl = "https://example.com/photo.jpg", includedServiceIds = listOf("night"), status = "PUBLICADO")

    @Test fun countsRealCatalogAvailabilityAndProfilesByRole() {
        val result = calculator.calculate(listOf(provider), listOf(service), listOf(destination), listOf(pack),
            listOf("cliente", "cliente", "gerente", "rol_antiguo"), 1L)
        assertEquals(1, result.activeProviders)
        assertEquals(1, result.availableServices)
        assertEquals(1, result.publishedPackages)
        assertEquals(mapOf("Cusco" to 1), result.servicesByDestination)
        assertEquals(4, result.totalUsers)
        assertEquals(1, result.usersByRole[UserRole.UNKNOWN.name])
        assertTrue(result.alerts.isEmpty())
    }

    @Test fun unavailableSupplierDisablesCoverageAndFlagsPublishedPackages() {
        val result = calculator.calculate(listOf(provider.copy(active = false)), listOf(service),
            listOf(destination), listOf(pack), emptyList())
        assertEquals(0, result.availableServices)
        assertTrue(result.servicesByDestination.isEmpty())
        assertEquals(1, result.alerts.first { it.area == CatalogArea.SERVICES }.count)
        assertTrue(result.alerts.any { it.title == "Paquetes publicados por revisar" && it.count == 1 })
    }

    @Test fun missingRelationsAndInvalidPricesNeverBecomeAvailableServices() {
        val services = listOf(service.copy(providerId = "missing"), service.copy(id = "zero", unitCost = 0.0),
            service.copy(id = "nan", unitCost = Double.NaN), service.copy(id = "orphan", destinationId = "missing"))
        val result = calculator.calculate(listOf(provider), services, listOf(destination), emptyList(), emptyList())
        assertEquals(0, result.availableServices)
        assertEquals(4, result.alerts.first { it.area == CatalogArea.SERVICES }.count)
    }

    @Test fun draftsAndArchivedDestinationsCannotSupplyPublishedPackages() {
        val result = calculator.calculate(listOf(provider), listOf(service),
            listOf(destination.copy(status = "BORRADOR")), listOf(pack, pack.copy(id = "draft", status = "BORRADOR")), emptyList())
        assertEquals(0, result.publishedDestinations)
        assertEquals(0, result.availableServices)
        assertEquals(1, result.packagesByStatus["BORRADOR"])
        assertTrue(result.alerts.any { it.area == CatalogArea.DESTINATIONS })
    }

    @Test fun emptyCatalogIsAValidEmptyResponseWithNoInventedActivity() {
        val result = calculator.calculate(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), 1L)
        assertTrue(result.hasData)
        assertEquals(0, result.totalProviders)
        assertEquals(0, result.totalUsers)
        assertTrue(result.alerts.isEmpty())
        assertTrue(result.servicesByDestination.isEmpty())
    }
}
