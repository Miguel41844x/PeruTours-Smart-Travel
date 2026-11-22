package com.example.perutours.ui.screens.quotation

import com.example.perutours.data.model.QuotationServiceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class QuotationValidatorTest {

    @Test
    fun acceptsSixServicesAndRejectsOverflowWithoutDroppingItems() {
        val services = List(6) {
            QuotationServiceItem(id = "service_$it", unitCost = 30.0, quantity = 2)
        }
        assertNull(QuotationValidator.validateServices(services))
        assertNotNull(QuotationValidator.validateServices(services + services.first()))
        assertEquals(6, services.size)
    }

    @Test
    fun rejectsNonFiniteCostsAndQuantitiesThatFirestoreWouldReject() {
        for (cost in listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, 1_000_001.0)) {
            assertNotNull(QuotationValidator.validateServices(listOf(
                QuotationServiceItem(unitCost = cost, quantity = 1)
            )))
        }
        assertNotNull(QuotationValidator.validateServices(listOf(
            QuotationServiceItem(unitCost = 30.0, quantity = 101)
        )))
    }

    @Test
    fun validityDaysAcceptConfiguredBoundaries() {
        assertNull(QuotationValidator.validateValidityDays(1))
        assertNull(QuotationValidator.validateValidityDays(30))
    }

    @Test
    fun validityDaysRejectValuesOutsideConfiguredRange() {
        assertEquals(
            "La vigencia debe encontrarse entre 1 y 30 días.",
            QuotationValidator.validateValidityDays(0)
        )
        assertEquals(
            "La vigencia debe encontrarse entre 1 y 30 días.",
            QuotationValidator.validateValidityDays(31)
        )
    }

    @Test
    fun conditionsAcceptConfiguredLengthLimit() {
        assertNull(
            QuotationValidator.validateConditions(
                "a".repeat(QuotationValidator.MAX_CONDITIONS_LENGTH)
            )
        )
    }

    @Test
    fun conditionsRejectTextOverConfiguredLengthLimit() {
        assertEquals(
            "Las condiciones no pueden superar 2000 caracteres.",
            QuotationValidator.validateConditions(
                "a".repeat(QuotationValidator.MAX_CONDITIONS_LENGTH + 1)
            )
        )
    }
}
