package com.example.perutours.ui.screens.recommendation

import com.example.perutours.data.model.Recommendation
import com.example.perutours.data.model.RecommendationSource
import com.example.perutours.data.repository.RecommendationRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun showsLoadingThenSuccessWhenRepositoryReturnsItems() = runTest(dispatcher) {
        val viewModel = RecommendationViewModel(
            fakeRepository(Result.success(sampleRecommendations()))
        )

        // Estado inicial inmediato (antes de que corra la corrutina).
        assertTrue(viewModel.uiState.value is RecommendationUiState.Loading)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is RecommendationUiState.Success)
        assertEquals(2, (state as RecommendationUiState.Success).recommendations.size)
    }

    @Test
    fun showsEmptyWhenRepositoryReturnsNoItems() = runTest(dispatcher) {
        val viewModel = RecommendationViewModel(
            fakeRepository(Result.success(emptyList()))
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is RecommendationUiState.Empty)
    }

    @Test
    fun showsFriendlyErrorWhenRepositoryFails() = runTest(dispatcher) {
        val viewModel = RecommendationViewModel(
            fakeRepository(Result.failure(IllegalStateException("sin red")))
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is RecommendationUiState.Error)
        assertEquals(
            "No pudimos generar tus recomendaciones. Revisa tu conexión.",
            (state as RecommendationUiState.Error).message
        )
    }

    @Test
    fun retryAfterErrorRecoversWithFreshResult() = runTest(dispatcher) {
        var current: Result<List<Recommendation>> =
            Result.failure(IllegalStateException("sin red"))
        val repository = mockk<RecommendationRepository> {
            coEvery { getRecommendations(any()) } coAnswers { current }
        }
        val viewModel = RecommendationViewModel(repository)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is RecommendationUiState.Error)

        current = Result.success(sampleRecommendations())
        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is RecommendationUiState.Success)
    }

    @Test
    fun refreshForcesANewRepositoryCallIgnoringCache() = runTest(dispatcher) {
        var callCount = 0
        var lastForceRefresh = false
        val repository = mockk<RecommendationRepository> {
            coEvery { getRecommendations(any()) } coAnswers {
                callCount++
                lastForceRefresh = firstArg()
                Result.success(sampleRecommendations())
            }
        }
        val viewModel = RecommendationViewModel(repository)

        advanceUntilIdle()
        val callsAfterInit = callCount
        assertEquals(1, callsAfterInit)

        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(lastForceRefresh)
        assertEquals(callsAfterInit + 1, callCount)
    }

    @Test
    fun successStateKeepsAiAndLocalSources() = runTest(dispatcher) {
        val viewModel = RecommendationViewModel(
            fakeRepository(Result.success(sampleRecommendations()))
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value as RecommendationUiState.Success
        val sources = state.recommendations.map { it.source }
        assertTrue(RecommendationSource.AI in sources)
        assertTrue(RecommendationSource.LOCAL in sources)
        assertFalse(state.recommendations.any { it.destinationId.isBlank() })
    }

    private fun fakeRepository(
        result: Result<List<Recommendation>>
    ): RecommendationRepository = mockk {
        coEvery { getRecommendations(any()) } returns result
    }

    private fun sampleRecommendations(): List<Recommendation> = listOf(
        Recommendation(
            destinationId = "1",
            title = "Machu Picchu",
            reason = "Encaja con tu tipo de viaje Cultura.",
            source = RecommendationSource.AI
        ),
        Recommendation(
            destinationId = "2",
            title = "Trekking al Ausangate",
            reason = "Encaja con tu presupuesto Económico.",
            source = RecommendationSource.LOCAL
        )
    )
}
