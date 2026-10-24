package app.twicast.tv.navigation

import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.AuthState
import app.twicast.tv.testing.FakeAuthRepository
import app.twicast.tv.testing.FakeStreamsRepository
import app.twicast.tv.testing.MainDispatcherRule
import app.twicast.tv.testing.stream
import app.twicast.tv.testing.testUser
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val streams = FakeStreamsRepository(followed = AppResult.Success(listOf(stream("a"), stream("b"))))

    @Test
    fun `the sidebar counts live followed channels and refreshes every 3 minutes`() = runTest(mainRule.dispatcher) {
        val viewModel = AppViewModel(FakeAuthRepository(AuthState.SignedIn(testUser)), streams)
        backgroundScope.launch { viewModel.liveFollowedCount.collect {} }
        runCurrent()
        assertEquals(2, viewModel.liveFollowedCount.value)

        streams.followed = AppResult.Success(listOf(stream("a"), stream("b"), stream("c")))
        advanceTimeBy(LiveCountRefreshMillis - 1)
        assertEquals("not yet", 2, viewModel.liveFollowedCount.value)
        advanceTimeBy(2)
        assertEquals(3, viewModel.liveFollowedCount.value)
    }

    @Test
    fun `a failed refresh keeps the last count`() = runTest(mainRule.dispatcher) {
        val viewModel = AppViewModel(FakeAuthRepository(AuthState.SignedIn(testUser)), streams)
        backgroundScope.launch { viewModel.liveFollowedCount.collect {} }
        runCurrent()
        streams.followed = AppResult.Failure(AppError.Network)
        advanceTimeBy(LiveCountRefreshMillis + 1)
        assertEquals(2, viewModel.liveFollowedCount.value)
    }

    @Test
    fun `signed out there is no count`() = runTest(mainRule.dispatcher) {
        val viewModel = AppViewModel(FakeAuthRepository(AuthState.SignedOut), streams)
        backgroundScope.launch { viewModel.liveFollowedCount.collect {} }
        runCurrent()
        assertEquals(0, viewModel.liveFollowedCount.value)
    }
}
