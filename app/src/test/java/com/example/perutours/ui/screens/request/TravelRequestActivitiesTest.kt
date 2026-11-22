package com.example.perutours.ui.screens.request

import com.example.perutours.data.location.DetectedLocation
import com.example.perutours.data.location.LocationDataSource
import com.example.perutours.data.model.CatalogService
import com.example.perutours.data.model.TourDestination
import com.example.perutours.data.model.TravelRequest
import com.example.perutours.data.model.TourPackage
import com.example.perutours.data.repository.PackageRepository
import com.example.perutours.data.repository.DestinationCatalogRepository
import com.example.perutours.data.repository.ServiceCatalogRepository
import com.example.perutours.data.repository.TravelRequestDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TravelRequestActivitiesTest {
    private val dispatcher = StandardTestDispatcher()
    private val arequipa = TourDestination(
        id = "arequipa", name = "Arequipa", department = "Arequipa",
        latitude = -16.40, longitude = -71.53, status = "PUBLICADO"
    )
    private val colca = CatalogService(
        id = "colca", name = "Colca full day", destinationId = arequipa.id, requestable = true
    )
    private val location = mockk<LocationDataSource>()
    private val requests = mockk<TravelRequestDataSource>()
    private val destinations = mockk<DestinationCatalogRepository>()
    private val services = mockk<ServiceCatalogRepository>()
    private val packages = mockk<PackageRepository>()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { destinations.getPublishedDestinations() } returns listOf(arequipa)
        coEvery { services.getRequestableActivities(arequipa.id) } returns listOf(colca)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun savesOptionalActivityAsItsOwnField() = runTest(dispatcher) {
        val saved = slot<TravelRequest>()
        coEvery { requests.save(capture(saved)) } answers { saved.captured.copy(id = "request") }
        coEvery { location.findPlace("Lima") } returns DetectedLocation("Lima, Perú", -12.04, -77.04)
        val model = viewModel()
        model.onDestinationChanged("Arequipa")
        advanceUntilIdle()
        assertEquals(listOf(colca), model.uiState.value.availableActivities)
        model.toggleActivity(colca.id)
        model.onOriginCityChanged("Lima")
        val now = System.currentTimeMillis()
        model.onDepartureChanged(now + 86_400_000L)
        model.onReturnChanged(now + 172_800_000L)
        model.onNotesChanged("Hotel céntrico")
        model.submitRequest()
        advanceUntilIdle()

        assertEquals(arequipa.id, saved.captured.destinationId)
        assertEquals(listOf(colca.id), saved.captured.requestedServiceIds)
        assertEquals("Hotel céntrico", saved.captured.notes)
        coVerify(exactly = 0) { location.findPlace("Arequipa") }
    }

    @Test
    fun changingDestinationClearsItsOptionalActivities() = runTest(dispatcher) {
        val model = viewModel()
        model.onDestinationChanged("Are")
        advanceUntilIdle()
        assertEquals(listOf(arequipa), model.uiState.value.destinationSuggestions)
        model.selectCatalogDestination(arequipa)
        advanceUntilIdle()
        model.toggleActivity(colca.id)
        model.onDestinationChanged("Cusco")
        advanceUntilIdle()

        assertEquals(emptyList<String>(), model.uiState.value.requestedServiceIds)
        assertEquals(emptyList<CatalogService>(), model.uiState.value.availableActivities)
    }

    @Test
    fun packageInclusionsAndExtrasTogetherStayWithinTheQuotationLimit() = runTest(dispatcher) {
        val ids = List(5) { "included_" + it }
        coEvery { packages.getPackage("pkg") } returns TourPackage(
            id = "pkg", destinationId = arequipa.id, title = "Arequipa",
            status = "PUBLICADO", includedServiceIds = ids
        )
        coEvery { destinations.getDestination(arequipa.id) } returns arequipa
        val other = colca.copy(id = "sillar")
        coEvery { services.getRequestableActivities(arequipa.id) } returns listOf(colca, other)
        val model = viewModel()
        model.prefillFromPackage("pkg")
        advanceUntilIdle()
        assertEquals(1, model.uiState.value.optionalActivityLimit)
        model.toggleActivity(colca.id)
        model.toggleActivity(other.id)
        assertEquals(listOf(colca.id), model.uiState.value.requestedServiceIds)
    }

    private fun viewModel() = TravelRequestViewModel(
        requests, location, TravelRequestPlaceResolver(location), packages, destinations, services
    )
}
