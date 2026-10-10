package com.example.perutours.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuotationStatusTest {

    @Test
    fun quotedAndObservedQuotationsCanBeAnswered() {
        assertTrue(Quotation.canClientRespond(Quotation.STATUS_QUOTED))
        assertTrue(Quotation.canClientRespond(Quotation.STATUS_OBSERVED))
    }

    @Test
    fun acceptedAndCancelledQuotationsAreFinal() {
        assertFalse(Quotation.canClientRespond(Quotation.STATUS_ACCEPTED))
        assertFalse(Quotation.canClientRespond(Quotation.STATUS_CANCELLED))
    }
}
