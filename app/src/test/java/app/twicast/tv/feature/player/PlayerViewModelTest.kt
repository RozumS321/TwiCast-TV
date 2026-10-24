package app.twicast.tv.feature.player

import app.twicast.tv.core.device.DeviceProfile
import app.twicast.tv.data.model.AppError
import app.twicast.tv.data.model.AppResult
import app.twicast.tv.data.model.PlayerEngine
import app.twicast.tv.data.repository.PlaybackRepository
import app.twicast.tv.data.repository.PlaybackSource
import app.twicast.tv.data.repository.PlayerSettingsRepository
import app.twicast.tv.feature.player.components.EmbedState
import app.twicast.tv.feature.player.components.VideoQuality
import app.twicast.tv.testing.FakeStreamsRepository
import app.twicast.tv.testing.FakeWatchProgressRepository
import app.twicast.tv.testing.FakeWatchHistoryRepository
import app.twicast.tv.data.repository.WatchProgress
import app.twicast.tv.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    override val openChatWithStream = MutableStateFlow(false)
    override val notifyLiveStarts = MutableStateFlow(true)
    override suspend fun setNotifyLiveStarts(enabled: Boolean) {
        notifyLiveStarts.value = enabled
    }
    override suspend fun setOpenChatWithStream(open: Boolean) {
        openChatWithStream.value = open
    }
    override val preferredQualityUpdates get() = MutableStateFlow(stored)
    // The embed by default: most tests here are about the shared controls, not the player choice.
    override val playerEngine = MutableStateFlow(PlayerEngine.Twitch)
    override suspend fun setPlayerEngine(engine: PlayerEngine) {
        playerEngine.value = engine
    }
    override suspend fun preferredQuality(): String? = stored
    override suspend fun setPreferredQuality(qualityId: String) {
        stored = qualityId
    }
}

private class FakePlayback(var result: AppResult<String> = AppResult.Success("https://usher.test/live.m3u8")) : PlaybackRepository {
    val requests = mutableListOf<PlaybackSource>()
    override suspend fun playbackUrl(source: PlaybackSource): AppResult<String> = result.also { requests += source }
}

class PlayerViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val settings = FakePlayerSettings()
    private val playback = FakePlayback()
    private val streams = FakeStreamsRepository()
    private val watch = FakeWatchProgressRepository()
    private val history = FakeWatchHistoryRepository()
    private val device = object : DeviceProfile { override var isLowMemory = false }
    private val destination = PlayerDestination("sasavot", "sasavot", "Just Chatting", 15_300, null)

    private fun TestScope.viewModel(): PlayerViewModel =
        PlayerViewModel(destination, settings, playback, streams, watch, history, device).also { runCurrent() }

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
    fun `TwiCast player resolves the stream playlist`() = runTest(mainRule.dispatcher) {
        settings.playerEngine.value = PlayerEngine.TwiCast
        val state = viewModel().uiState.value

        assertEquals(PlayerEngine.TwiCast, state.engine)
        assertEquals("https://usher.test/live.m3u8", state.streamUrl)
        assertEquals(listOf<PlaybackSource>(PlaybackSource.Live("sasavot")), playback.requests)
    }

    @Test
    fun `refused token or a failing TwiCast player falls back to the Twitch player`() = runTest(mainRule.dispatcher) {
        settings.playerEngine.value = PlayerEngine.TwiCast
        playback.result = AppResult.Failure(AppError.NotFound)
        assertEquals(PlayerEngine.Twitch, viewModel().uiState.value.engine)

        playback.result = AppResult.Success("https://usher.test/live.m3u8")
        val viewModel = viewModel()
        viewModel.onEmbedStateChanged(EmbedState.Error)
        assertEquals(PlayerEngine.Twitch, viewModel.uiState.value.engine)
        assertEquals(PlaybackStatus.Loading, viewModel.uiState.value.status)
    }

    @Test
    fun `no network is an error, not a fallback`() = runTest(mainRule.dispatcher) {
        settings.playerEngine.value = PlayerEngine.TwiCast
        playback.result = AppResult.Failure(AppError.Network)
        val state = viewModel().uiState.value
        assertEquals(PlayerEngine.TwiCast, state.engine)
        assertEquals(PlaybackStatus.Error, state.status)
    }

    @Test
    fun `a ceiling from Settings caps the height and is passed on to the player`() = runTest(mainRule.dispatcher) {
        settings.stored = "480p"
        val state = viewModel().uiState.value
        assertEquals("480p", state.preferredQualityId)
        assertEquals(480, state.maxVideoHeight)
    }

    @Test
    fun `the lower of the Settings ceiling and the low-memory cap wins`() = runTest(mainRule.dispatcher) {
        device.isLowMemory = true
        settings.stored = "1080p"
        assertEquals(RecoveryMaxVideoHeight, viewModel().uiState.value.maxVideoHeight)
    }

    @Test
    fun `chat opens with a live stream when Settings say so, never with a recording`() = runTest(mainRule.dispatcher) {
        settings.openChatWithStream.value = true
        assertTrue(viewModel().uiState.value.chatVisible)

        val recording = PlayerViewModel(destination.copy(videoId = "123"), settings, playback, streams, watch, history, device).also { runCurrent() }
        assertFalse(recording.uiState.value.chatVisible)
    }

    @Test
    fun `a stalled live stream whose broadcast ended says so and keeps the controls up`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.onEmbedStateChanged(EmbedState.Playing)
        viewModel.onEmbedStateChanged(EmbedState.Buffering)

        advanceTimeBy(StallCheckMillis + 1)
        assertEquals("still broadcasting: keep waiting", PlaybackStatus.Playing, viewModel.uiState.value.status)

        streams.live = AppResult.Success(false)
        advanceTimeBy(StallCheckMillis + 1)
        assertEquals(PlaybackStatus.Ended, viewModel.uiState.value.status)
        assertEquals(PlayerOverlay.Controls, viewModel.uiState.value.overlay)

        advanceTimeBy(ControlsAutoHideMillis + 1)
        assertEquals("the controls stay", PlayerOverlay.Controls, viewModel.uiState.value.overlay)
    }

    @Test
    fun `playback resuming stops the check`() = runTest(mainRule.dispatcher) {
        streams.live = AppResult.Success(false)
        val viewModel = viewModel()
        viewModel.onEmbedStateChanged(EmbedState.Playing)
        viewModel.onEmbedStateChanged(EmbedState.Buffering)
        viewModel.onEmbedStateChanged(EmbedState.Playing)

        advanceTimeBy(StallCheckMillis * 2)
        assertEquals(PlaybackStatus.Playing, viewModel.uiState.value.status)
    }

    @Test
    fun `offline while watching is an ended stream, offline from the start is just offline`() = runTest(mainRule.dispatcher) {
        val watching = viewModel()
        watching.onEmbedStateChanged(EmbedState.Playing)
        watching.onEmbedStateChanged(EmbedState.Offline)
        assertEquals(PlaybackStatus.Ended, watching.uiState.value.status)

        val opened = viewModel()
        opened.onEmbedStateChanged(EmbedState.Offline)
        assertEquals(PlaybackStatus.Offline, opened.uiState.value.status)
    }

    @Test
    fun `malformed login never reaches the embed`() = runTest(mainRule.dispatcher) {
        val viewModel = PlayerViewModel(destination.copy(channelLogin = "a&b=c"), settings, playback, streams, watch, history, device)
        assertFalse(viewModel.uiState.value.canPlay)
    }

    // A 7-hour recording, 1 hour in.
    private fun TestScope.recording(): PlayerViewModel =
        PlayerViewModel(destination.copy(videoId = "2512345678"), settings, playback, streams, watch, history, device).also {
            runCurrent()
            it.onProgress(positionMs = 3_600_000, durationMs = 25_200_000)
        }

    private fun TestScope.sentSeeks(viewModel: PlayerViewModel): MutableList<Long> {
        val sent = mutableListOf<Long>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.seekRequests.collect { sent += it } }
        return sent
    }

    @Test
    fun `winding a recording from the bare video shows only the seek bar and sends one seek`() = runTest(mainRule.dispatcher) {
        val viewModel = recording()
        val sent = sentSeeks(viewModel)

        viewModel.onSeekStep(forward = true, repeat = 0)
        viewModel.onSeekStep(forward = true, repeat = 0)
        viewModel.onSeekStep(forward = false, repeat = 0)
        val state = viewModel.uiState.value
        assertEquals(PlayerOverlay.Seek, state.overlay)
        assertEquals(3_610_000L, state.shownPositionMs)
        assertEquals("the bubble shows the whole jump", 10_000L, state.seekDeltaMs)
        assertTrue("nothing sent while the remote is busy", sent.isEmpty())

        advanceTimeBy(SeekCommitMillis + 1)
        assertEquals(listOf(3_610_000L), sent)
        assertEquals(3_610_000L, viewModel.uiState.value.progress?.positionMs)
        assertEquals(null, viewModel.uiState.value.seekDeltaMs)

        advanceTimeBy(SeekOverlayMillis + 1)
        assertEquals(PlayerOverlay.Hidden, viewModel.uiState.value.overlay)
    }

    @Test
    fun `holding the key winds in bigger steps`() {
        assertEquals(10_000L, seekStepMillis(0))
        assertEquals(30_000L, seekStepMillis(10))
        assertEquals(60_000L, seekStepMillis(30))
    }

    @Test
    fun `winding stops at the start and the end of the recording`() = runTest(mainRule.dispatcher) {
        val viewModel = recording()
        repeat(100) { viewModel.onSeekStep(forward = false, repeat = 40) }
        assertEquals(0L, viewModel.uiState.value.shownPositionMs)
        repeat(500) { viewModel.onSeekStep(forward = true, repeat = 40) }
        assertEquals(25_200_000L, viewModel.uiState.value.shownPositionMs)
    }

    @Test
    fun `winding in the controls keeps them up, and Back sends the dialled position`() = runTest(mainRule.dispatcher) {
        val viewModel = recording()
        val sent = sentSeeks(viewModel)
        viewModel.onRemoteActivity()
        viewModel.onSeekStep(forward = false, repeat = 0)
        assertEquals(PlayerOverlay.Controls, viewModel.uiState.value.overlay)

        assertTrue(viewModel.onBack())
        assertEquals(listOf(3_590_000L), sent)
        assertEquals(PlayerOverlay.Hidden, viewModel.uiState.value.overlay)
    }

    @Test
    fun `a live stream does not wind`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.onProgress(positionMs = 10_000, durationMs = 20_000)
        viewModel.onSeekStep(forward = true, repeat = 0)
        assertFalse(viewModel.uiState.value.canSeek)
        assertEquals(PlayerOverlay.Hidden, viewModel.uiState.value.overlay)
    }

    private fun TestScope.resumed(positionMs: Long): PlayerViewModel {
        watch.progress.value = mapOf("2512345678" to WatchProgress(positionMs, durationMs = 25_200_000, updatedAtMillis = 1))
        return PlayerViewModel(destination.copy(videoId = "2512345678"), settings, playback, streams, watch, history, device).also { runCurrent() }
    }

    @Test
    fun `a started recording plays on from where the viewer stopped`() = runTest(mainRule.dispatcher) {
        val state = resumed(positionMs = 5_050_000).uiState.value
        assertTrue("known before the player is made", state.settingsLoaded)
        assertEquals(5_050_000L, state.startPositionMs)
        assertEquals(5_050_000L, state.resumedFromMs)
        assertTrue(state.resumeNotice)
    }

    @Test
    fun `a finished recording starts over`() = runTest(mainRule.dispatcher) {
        val state = resumed(positionMs = 25_200_000 - 60_000).uiState.value
        assertEquals(null, state.startPositionMs)
        assertFalse(state.resumeNotice)
    }

    @Test
    fun `the resume notice goes 5 s after the recording plays`() = runTest(mainRule.dispatcher) {
        val viewModel = resumed(positionMs = 5_050_000)
        advanceTimeBy(ResumeNoticeMillis * 2)
        assertTrue("waits for the picture", viewModel.uiState.value.resumeNotice)

        viewModel.onEmbedStateChanged(EmbedState.Playing)
        advanceTimeBy(ResumeNoticeMillis + 1)
        assertFalse(viewModel.uiState.value.resumeNotice)
        assertEquals("«С начала» stays", 5_050_000L, viewModel.uiState.value.resumedFromMs)
    }

    @Test
    fun `the position is saved at most every 10 s, and right after a seek`() = runTest(mainRule.dispatcher) {
        val viewModel = recording()
        runCurrent()
        assertEquals(listOf(Triple("2512345678", 3_600_000L, 25_200_000L)), watch.saved)

        viewModel.onProgress(3_601_000, 25_200_000)
        runCurrent()
        assertEquals(1, watch.saved.size)

        advanceTimeBy(WatchProgressSaveMillis + 1)
        viewModel.onProgress(3_611_000, 25_200_000)
        runCurrent()
        assertEquals(3_611_000L, watch.saved.last().second)

        viewModel.onSeekStep(forward = true, repeat = 0)
        advanceTimeBy(SeekCommitMillis + 1)
        assertEquals(3_621_000L, watch.saved.last().second)
    }

    @Test
    fun `from the start goes to 0 00 and forgets the position`() = runTest(mainRule.dispatcher) {
        val viewModel = resumed(positionMs = 5_050_000)
        val sent = sentSeeks(viewModel)
        viewModel.onProgress(5_050_000, 25_200_000)
        advanceTimeBy(WatchProgressSaveMillis + 1)

        viewModel.onRestart()
        runCurrent()
        assertEquals(listOf(0L), sent)
        assertEquals(null, viewModel.uiState.value.resumedFromMs)
        assertEquals(0L, viewModel.uiState.value.shownPositionMs)
        assertFalse("under 30 s is forgotten", "2512345678" in watch.progress.value)
    }

    @Test
    fun `a live stream saves no position`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.onProgress(10_000, 20_000)
        runCurrent()
        assertTrue(watch.saved.isEmpty())
    }

    @Test
    fun `opening a stream or a recording puts it in the history, a clip does not`() = runTest(mainRule.dispatcher) {
        viewModel()
        PlayerViewModel(destination.copy(videoId = "v1", title = "Ранкед"), settings, playback, streams, watch, history, device)
        PlayerViewModel(destination.copy(clipSlug = "Clip"), settings, playback, streams, watch, history, device)
        runCurrent()
        val entries = history.entries.value
        assertEquals(listOf("v1", "live:sasavot"), entries.map { it.key })
        assertEquals("Ранкед", entries.first().title)
    }
}
