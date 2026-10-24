package com.example.perutours.ui.screens.quotation

import com.example.perutours.data.model.CatalogService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuotationCatalogFilterTest {

    private val services = listOf(
        CatalogService(id = "arequipa", city = "Arequipa"),
        CatalogService(id = "junin", city = "Junín"),
        CatalogService(id = "generic", city = "")
    )

    @Test
    fun returnsOnlyServicesForDestinationAndGenericServices() {
        val result = QuotationCatalogFilter.forDestination(
            services = services,
            destination = "Junin, Perú"
        )

        assertEquals(listOf("junin", "generic"), result.map { it.id })
    }

    @Test
    fun doesNotShowServicesFromAnotherCity() {
        val result = QuotationCatalogFilter.forDestination(
            services = services,
            destination = "Chiclayo, Lambayeque, Perú"
        )

        assertEquals(listOf("generic"), result.map { it.id })
        assertTrue(result.none { it.city == "Arequipa" })
    }
}
