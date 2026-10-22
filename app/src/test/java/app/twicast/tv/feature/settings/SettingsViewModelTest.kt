package app.twicast.tv.feature.settings

import app.twicast.tv.data.model.PlayerEngine
import app.twicast.tv.data.repository.PlayerSettingsRepository
import app.twicast.tv.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private class FakeSettings : PlayerSettingsRepository {
    override val playerEngine = MutableStateFlow(PlayerEngine.TwiCast)
    override suspend fun preferredQuality(): String? = null
    override suspend fun setPreferredQuality(qualityId: String) = Unit
    override suspend fun setPlayerEngine(engine: PlayerEngine) {
        playerEngine.value = engine
    }
}

class SettingsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `shows the stored player and saves a new choice`() = runTest(mainRule.dispatcher) {
        val settings = FakeSettings()
        val viewModel = SettingsViewModel(settings)
        backgroundScope.launch { viewModel.playerEngine.collect {} }

        assertEquals(PlayerEngine.TwiCast, viewModel.playerEngine.value)
        viewModel.onPlayerSelected(PlayerEngine.Twitch)
        assertEquals(PlayerEngine.Twitch, settings.playerEngine.value)
        assertEquals(PlayerEngine.Twitch, viewModel.playerEngine.value)
    }
}
