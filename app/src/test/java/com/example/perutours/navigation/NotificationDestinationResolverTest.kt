package com.example.perutours.navigation

import org.junit.Assert.*
import org.junit.Test

class NotificationDestinationResolverTest {
    @Test fun remindersAndPaymentUpdatesOpenTheirOwnScreen() {
        assertEquals("trip/res_1", NotificationDestinationResolver.resolve("trip_reminder", null, "res_1"))
        assertEquals("customer_reservation/res_1", NotificationDestinationResolver.resolve("payment_review", null, "res_1"))
        assertEquals("agent_quotation_detail/quote_1", NotificationDestinationResolver.resolve("quotation_response", "quote_1", null))
    }

    @Test fun invalidTypesAndInjectedPathsCannotOpenArbitraryScreens() {
        assertNull(NotificationDestinationResolver.resolve("admin_dashboard", "quote", "res"))
        assertNull(NotificationDestinationResolver.resolve("trip_reminder", null, "../admin"))
        assertNull(NotificationDestinationResolver.resolve("quotation", "", null))
    }
}
