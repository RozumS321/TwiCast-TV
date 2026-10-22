package app.twicast.tv.feature.auth

import app.cash.turbine.test
import app.twicast.tv.core.network.Clock
import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.repository.DeviceAuthorization
import app.twicast.tv.data.repository.DevicePollResult
import app.twicast.tv.testing.FakeAuthRepository
import app.twicast.tv.testing.MainDispatcherRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private const val IntervalMillis = 5_000L
private const val LifetimeMillis = 60_000L

class LoginViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val authorization = DeviceAuthorization(
        deviceCode = "device",
        userCode = "WDJBMJHT",
        verificationUri = "https://www.twitch.tv/activate?device-code=WDJBMJHT",
        expiresAtMillis = LifetimeMillis,
        pollIntervalMillis = IntervalMillis,
    )
    private val auth = FakeAuthRepository(startResult = AppResult.Success(authorization))

    /** The view model's clock follows the test's virtual time. */
    private fun TestScope.viewModel() = LoginViewModel(auth, Clock { testScheduler.currentTime })

    @Test
    fun `shows the code and keeps polling while pending`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(LoginUiState.Loading, awaitItem())
            val waiting = awaitItem() as LoginUiState.WaitingForConfirmation
            assertEquals("WDJBMJHT", waiting.userCode)

            advanceTimeBy(IntervalMillis * 3 + 1)
            assertEquals(3, auth.pollCount)
            expectNoEvents()
        }
    }

    @Test
    fun `code expiry shows expired`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.uiState.test {
            skipItems(2)
            advanceTimeBy(LifetimeMillis + IntervalMillis)
            assertEquals(LoginUiState.Expired, awaitItem())
        }
    }

    @Test
    fun `slow down increases the poll interval`() = runTest(mainRule.dispatcher) {
        auth.pollResults = listOf(DevicePollResult.SlowDown, DevicePollResult.Pending)
        viewModel()
        runCurrent()
        advanceTimeBy(IntervalMillis + 1)
        assertEquals(1, auth.pollCount)
        // Next poll comes after 5 s + 5 s, not after 5 s.
        advanceTimeBy(IntervalMillis)
        assertEquals(1, auth.pollCount)
        advanceTimeBy(IntervalMillis)
        assertEquals(2, auth.pollCount)
    }

    @Test
    fun `network blip keeps polling, other failures show an error`() = runTest(mainRule.dispatcher) {
        auth.pollResults = listOf(
            DevicePollResult.Failed(AppError.Network),
            DevicePollResult.Failed(AppError.Unknown),
        )
        val viewModel = viewModel()
        viewModel.uiState.test {
            skipItems(2)
            advanceTimeBy(IntervalMillis * 2 + 1)
            assertEquals(LoginUiState.Error(AppError.Unknown), awaitItem())
        }
    }

    @Test
    fun `failed code request shows an error and new code retries`() = runTest(mainRule.dispatcher) {
        auth.startResult = AppResult.Failure(AppError.Network)
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(LoginUiState.Loading, awaitItem())
            assertEquals(LoginUiState.Error(AppError.Network), awaitItem())

            auth.startResult = AppResult.Success(authorization)
            viewModel.onNewCode()
            assertEquals(LoginUiState.Loading, awaitItem())
            assertEquals("WDJBMJHT", (awaitItem() as LoginUiState.WaitingForConfirmation).userCode)
        }
    }
}
