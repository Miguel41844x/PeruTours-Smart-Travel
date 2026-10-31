package com.example.perutours.ui.screens.admin

import org.junit.Assert.assertEquals
import org.junit.Test

class AdminDashboardViewModelTest {

    @Test
    fun testConstants() {
        assertEquals(15, AdminDashboardConstants.DEFAULT_PAGE_SIZE)
        assertEquals(5, AdminDashboardConstants.AVAILABLE_FILTERS.size)
    }
}
