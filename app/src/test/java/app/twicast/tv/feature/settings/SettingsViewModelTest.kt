package app.twicast.tv.feature.settings

import app.twicast.tv.data.model.PlayerEngine
import app.twicast.tv.data.model.ThemeMode
import app.twicast.tv.data.repository.AppearanceSettingsRepository
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

private class FakeAppearance : AppearanceSettingsRepository {
    override val themeMode = MutableStateFlow(ThemeMode.System)
    override suspend fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }
}

class SettingsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `shows the stored player and saves a new choice`() = runTest(mainRule.dispatcher) {
        val settings = FakeSettings()
        val viewModel = SettingsViewModel(settings, FakeAppearance())
        backgroundScope.launch { viewModel.playerEngine.collect {} }

        assertEquals(PlayerEngine.TwiCast, viewModel.playerEngine.value)
        viewModel.onPlayerSelected(PlayerEngine.Twitch)
        assertEquals(PlayerEngine.Twitch, settings.playerEngine.value)
        assertEquals(PlayerEngine.Twitch, viewModel.playerEngine.value)
    }

    @Test
    fun `shows the stored theme and saves a new choice`() = runTest(mainRule.dispatcher) {
        val appearance = FakeAppearance()
        val viewModel = SettingsViewModel(FakeSettings(), appearance)
        backgroundScope.launch { viewModel.themeMode.collect {} }

        assertEquals(ThemeMode.System, viewModel.themeMode.value)
        viewModel.onThemeSelected(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, appearance.themeMode.value)
        assertEquals(ThemeMode.Dark, viewModel.themeMode.value)
    }
}
