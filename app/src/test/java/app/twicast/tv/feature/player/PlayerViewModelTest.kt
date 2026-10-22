package app.twicast.tv.feature.player

import app.twicast.tv.core.device.DeviceProfile
import app.twicast.tv.data.repository.PlayerSettingsRepository
import app.twicast.tv.feature.player.components.EmbedState
import app.twicast.tv.feature.player.components.VideoQuality
import app.twicast.tv.testing.MainDispatcherRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class FakePlayerSettings(var stored: String? = null) : PlayerSettingsRepository {
    override suspend fun preferredQuality(): String? = stored
    override suspend fun setPreferredQuality(qualityId: String) {
        stored = qualityId
    }
}

class PlayerViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val settings = FakePlayerSettings()
    private val device = object : DeviceProfile { override var isLowMemory = false }
    private val destination = PlayerDestination("sasavot", "sasavot", "Just Chatting", 15_300, null)

    private fun TestScope.viewModel(): PlayerViewModel =
        PlayerViewModel(destination, settings, device).also { runCurrent() }

    @Test
    fun `preferred quality is loaded before the embed may be created`() = runTest(mainRule.dispatcher) {
        settings.stored = "720p60"
        val state = viewModel().uiState.value
        assertTrue(state.settingsLoaded)
        assertEquals("720p60", state.preferredQualityId)
    }

    @Test
    fun `a key on bare video shows the controls, which hide after 4 s of silence`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.onRemoteActivity()
        assertEquals(PlayerOverlay.Controls, viewModel.uiState.value.overlay)

        advanceTimeBy(ControlsAutoHideMillis - 1)
        viewModel.onRemoteActivity()
        advanceTimeBy(ControlsAutoHideMillis - 1)
        assertEquals("activity restarts the timer", PlayerOverlay.Controls, viewModel.uiState.value.overlay)

        advanceTimeBy(2)
        assertEquals(PlayerOverlay.Hidden, viewModel.uiState.value.overlay)
    }

    @Test
    fun `the quality menu does not auto-hide`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.onRemoteActivity()
        viewModel.onOpenQuality()
        advanceTimeBy(ControlsAutoHideMillis * 2)
        assertEquals(PlayerOverlay.Quality, viewModel.uiState.value.overlay)
    }

    @Test
    fun `selecting a quality remembers it and returns to the video`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.onQualities(listOf(VideoQuality("auto", "Auto"), VideoQuality("480p30", "480p")), currentId = "auto")
        viewModel.onOpenQuality()

        viewModel.onQualitySelected("480p30")
        runCurrent()

        assertEquals(PlayerOverlay.Hidden, viewModel.uiState.value.overlay)
        assertEquals("480p", viewModel.uiState.value.currentQualityLabel)
        assertEquals("480p30", settings.stored)
    }

    @Test
    fun `back closes quality, then controls, then chat, then the screen`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.onToggleChat()
        viewModel.onRemoteActivity()
        viewModel.onOpenQuality()

        assertTrue(viewModel.onBack())
        assertEquals(PlayerOverlay.Controls, viewModel.uiState.value.overlay)
        assertTrue(viewModel.onBack())
        assertEquals(PlayerOverlay.Hidden, viewModel.uiState.value.overlay)
        assertTrue(viewModel.onBack())
        assertFalse(viewModel.uiState.value.chatVisible)
        assertFalse("nothing left to close: leave the player", viewModel.onBack())
    }

    @Test
    fun `renderer crash rebuilds the embed at 720p, then gives up`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.onEmbedStateChanged(EmbedState.Playing)

        viewModel.onRendererGone()
        with(viewModel.uiState.value) {
            assertEquals(1, embedAttempt)
            assertEquals(RecoveryMaxVideoHeight, maxVideoHeight)
            assertEquals(PlaybackStatus.Loading, status)
        }

        repeat(MaxEmbedRestarts) { viewModel.onRendererGone() }
        assertEquals(MaxEmbedRestarts, viewModel.uiState.value.embedAttempt)
        assertEquals(PlaybackStatus.Error, viewModel.uiState.value.status)
    }

    @Test
    fun `low-memory TVs start capped at 720p`() = runTest(mainRule.dispatcher) {
        device.isLowMemory = true
        assertEquals(RecoveryMaxVideoHeight, viewModel().uiState.value.maxVideoHeight)
    }

    @Test
    fun `malformed login never reaches the embed`() = runTest(mainRule.dispatcher) {
        val viewModel = PlayerViewModel(destination.copy(channelLogin = "a&b=c"), settings, device)
        assertFalse(viewModel.uiState.value.canPlay)
    }
}
