package app.twicast.tv.feature.home

import app.cash.turbine.test
import app.twicast.tv.core.network.Clock
import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.AuthState
import app.twicast.tv.testing.FakeAuthRepository
import app.twicast.tv.testing.FakeStreamsRepository
import app.twicast.tv.testing.MainDispatcherRule
import app.twicast.tv.testing.stream
import app.twicast.tv.testing.testUser
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class HomeViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val auth = FakeAuthRepository(initialState = AuthState.SignedIn(testUser))
    private val streams = FakeStreamsRepository()

    private val clock = Clock { mainRule.dispatcher.scheduler.currentTime }

    private fun viewModel() = HomeViewModel(auth, streams, Locale.forLanguageTag("ru-UA"), clock)

    @Test
    fun `loading then followed, recommended and regional rows`() = runTest(mainRule.dispatcher) {
        streams.followed = AppResult.Success(listOf(stream("1", categoryId = "cs"), stream("2", categoryId = "mc")))
        streams.inCategories = AppResult.Success(listOf(stream("3", categoryId = "cs")))
        streams.results = mapOf("ru" to AppResult.Success(listOf(stream("4"))))

        viewModel().uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            val content = awaitItem() as HomeUiState.Content
            assertEquals("nightowl", content.userName)
            assertEquals(listOf("1", "2"), content.followedLive.map { it.id })
            assertEquals(listOf("3"), content.recommended.map { it.id })
            assertEquals(listOf("4"), content.regional.map { it.id })
        }
    }

    @Test
    fun `recommendations use the followed streams' categories, most watched first`() = runTest(mainRule.dispatcher) {
        streams.followed = AppResult.Success(
            listOf(stream("1", categoryId = "small", viewers = 10), stream("2", categoryId = "big", viewers = 900)),
        )

        viewModel().uiState.test {
            skipItems(2)
            assertEquals(listOf("big", "small"), streams.requestedCategoryIds)
        }
    }

    @Test
    fun `viewer language comes from followed streams, not the device`() = runTest(mainRule.dispatcher) {
        streams.followed = AppResult.Success(
            listOf(stream("1", language = "uk"), stream("2", language = "uk"), stream("3", language = "en")),
        )

        viewModel().uiState.test {
            skipItems(1)
            assertEquals("uk", (awaitItem() as HomeUiState.Content).languageCode)
            // Both recommendations and "popular" are requested in that language.
            assertEquals(listOf("uk"), streams.requestedLanguages.distinct())
        }
    }

    @Test
    fun `no live followed channels falls back to the device language`() = runTest(mainRule.dispatcher) {
        streams.results = mapOf("ru" to AppResult.Success(listOf(stream("4"))))

        viewModel().uiState.test {
            skipItems(1)
            assertEquals("ru", (awaitItem() as HomeUiState.Content).languageCode)
        }
    }

    @Test
    fun `followed channels are not recommended`() = runTest(mainRule.dispatcher) {
        streams.followed = AppResult.Success(listOf(stream("1")))
        streams.inCategories = AppResult.Success(listOf(stream("1"), stream("5")))

        viewModel().uiState.test {
            skipItems(1)
            assertEquals(listOf("5"), (awaitItem() as HomeUiState.Content).recommended.map { it.id })
        }
    }

    @Test
    fun `failed followed request is an error`() = runTest(mainRule.dispatcher) {
        streams.followed = AppResult.Failure(AppError.Unauthorized)

        viewModel().uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            assertEquals(HomeUiState.Error(AppError.Unauthorized), awaitItem())
        }
    }

    @Test
    fun `nothing live anywhere is empty`() = runTest(mainRule.dispatcher) {
        viewModel().uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            assertEquals(HomeUiState.Empty("nightowl"), awaitItem())
        }
    }

    @Test
    fun `retry after an error loads again`() = runTest(mainRule.dispatcher) {
        streams.results = mapOf("ru" to AppResult.Failure(AppError.Network))
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            assertEquals(HomeUiState.Error(AppError.Network), awaitItem())

            streams.results = mapOf("ru" to AppResult.Success(listOf(stream("1"))))
            viewModel.onRetry()
            assertEquals(HomeUiState.Loading, awaitItem())
            assertEquals(listOf("1"), (awaitItem() as HomeUiState.Content).regional.map { it.id })
        }
    }

    @Test
    fun `coming back after a minute refreshes quietly, sooner does not`() = runTest(mainRule.dispatcher) {
        streams.followed = AppResult.Success(listOf(stream("1")))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onVisible()
        viewModel.onHidden()

        streams.followed = AppResult.Success(listOf(stream("1"), stream("2")))
        advanceTimeBy(HomeStaleMillis / 2)
        viewModel.onVisible()
        runCurrent()
        assertEquals(listOf("1"), (viewModel.uiState.value as HomeUiState.Content).followedLive.map { it.id })
        viewModel.onHidden()

        advanceTimeBy(HomeStaleMillis)
        viewModel.onVisible()
        assertTrue("no skeleton during a refresh", viewModel.uiState.value is HomeUiState.Content)
        runCurrent()
        assertEquals(listOf("1", "2"), (viewModel.uiState.value as HomeUiState.Content).followedLive.map { it.id })
        viewModel.onHidden()
    }

    @Test
    fun `home on screen refreshes by itself, a failed refresh keeps the rows`() = runTest(mainRule.dispatcher) {
        streams.followed = AppResult.Success(listOf(stream("1")))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onVisible()

        streams.followed = AppResult.Failure(AppError.Network)
        advanceTimeBy(HomeAutoRefreshMillis + 1)
        assertEquals(listOf("1"), (viewModel.uiState.value as HomeUiState.Content).followedLive.map { it.id })

        streams.followed = AppResult.Success(listOf(stream("3")))
        advanceTimeBy(HomeAutoRefreshMillis + 1)
        assertEquals(listOf("3"), (viewModel.uiState.value as HomeUiState.Content).followedLive.map { it.id })
        viewModel.onHidden()
    }
}
