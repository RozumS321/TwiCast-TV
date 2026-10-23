package app.twicast.tv.feature.settings

import app.twicast.tv.data.model.AppLanguage
import app.twicast.tv.data.model.PlayerEngine
import app.twicast.tv.data.model.ThemeMode
import app.twicast.tv.data.repository.AppearanceSettingsRepository
import app.twicast.tv.core.device.DeviceProfile
import app.twicast.tv.data.repository.LanguageSettingsRepository
import app.twicast.tv.data.repository.OptimizationSettingsRepository
import app.twicast.tv.data.repository.PlayerSettingsRepository
import app.twicast.tv.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class FakeSettings : PlayerSettingsRepository {
    override val playerEngine = MutableStateFlow(PlayerEngine.TwiCast)
    override val preferredQualityUpdates = MutableStateFlow<String?>(null)
    override val openChatWithStream = MutableStateFlow(false)
    override suspend fun preferredQuality(): String? = preferredQualityUpdates.value
    override suspend fun setPreferredQuality(qualityId: String) {
        preferredQualityUpdates.value = qualityId
    }
    override suspend fun setPlayerEngine(engine: PlayerEngine) {
        playerEngine.value = engine
    }
    override suspend fun setOpenChatWithStream(open: Boolean) {
        openChatWithStream.value = open
    }
}

private class FakeAppearance : AppearanceSettingsRepository {
    override val themeMode = MutableStateFlow(ThemeMode.System)
    override suspend fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }
}

private class FakeLanguages : LanguageSettingsRepository {
    var stored = AppLanguage.Russian
    override fun language() = stored
    override fun setLanguage(language: AppLanguage) {
        stored = language
    }
}

private class FakeOptimization : OptimizationSettingsRepository {
    override val warmTwitchPlayer = MutableStateFlow(false)
    override val freeMemoryInBackground = MutableStateFlow(true)
    override suspend fun setWarmTwitchPlayer(enabled: Boolean) {
        warmTwitchPlayer.value = enabled
    }
    override suspend fun setFreeMemoryInBackground(enabled: Boolean) {
        freeMemoryInBackground.value = enabled
    }
}

private val bigDevice = object : DeviceProfile { override val isLowMemory = false }

class SettingsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `shows the stored choices and saves new ones`() = runTest(mainRule.dispatcher) {
        val settings = FakeSettings()
        val appearance = FakeAppearance()
        val viewModel = SettingsViewModel(settings, appearance, FakeLanguages(), FakeOptimization(), bigDevice)
        backgroundScope.launch { viewModel.uiState.collect {} }

        val loaded = viewModel.uiState.value
        assertTrue(loaded.loaded)
        assertEquals(PlayerEngine.TwiCast, loaded.playerEngine)
        assertEquals(DefaultQuality.Auto, loaded.defaultQuality)
        assertEquals(ThemeMode.System, loaded.themeMode)
        assertEquals(false, loaded.openChatWithStream)

        viewModel.onPlayerSelected(PlayerEngine.Twitch)
        viewModel.onDefaultQualitySelected(DefaultQuality.P720)
        viewModel.onThemeSelected(ThemeMode.Dark)
        viewModel.onOpenChatChanged(true)

        assertEquals("720p", settings.preferredQualityUpdates.value)
        val saved = viewModel.uiState.value
        assertEquals(PlayerEngine.Twitch, saved.playerEngine)
        assertEquals(DefaultQuality.P720, saved.defaultQuality)
        assertEquals(ThemeMode.Dark, saved.themeMode)
        assertEquals(true, saved.openChatWithStream)
    }

    @Test
    fun `a new language is saved and asks for a redraw focused on the language, the same one does not`() {
        val languages = FakeLanguages()
        val viewModel = SettingsViewModel(FakeSettings(), FakeAppearance(), languages, FakeOptimization(), bigDevice)

        assertFalse(viewModel.onLanguageSelected(AppLanguage.Russian))
        assertTrue(viewModel.onLanguageSelected(AppLanguage.Ukrainian))
        assertEquals(AppLanguage.Ukrainian, languages.stored)
    }

    @Test
    fun `quality chosen in the player lights the chip of its height`() {
        assertEquals(DefaultQuality.Auto, DefaultQuality.of(null))
        assertEquals(DefaultQuality.Auto, DefaultQuality.of("auto"))
        assertEquals(DefaultQuality.P720, DefaultQuality.of("720p60"))
        assertEquals(DefaultQuality.P480, DefaultQuality.of("480p"))
        assertEquals(null, DefaultQuality.of("chunked"))
    }

    @Test
    fun `a language change keeps System open and hands the focus to the language once`() = runTest(mainRule.dispatcher) {
        val viewModel = SettingsViewModel(FakeSettings(), FakeAppearance(), FakeLanguages(), FakeOptimization(), bigDevice)
        backgroundScope.launch { viewModel.uiState.collect {} }

        assertEquals(SettingsSection.Player, viewModel.uiState.value.section)
        viewModel.onSectionFocused(SettingsSection.System)
        viewModel.onLanguageSelected(AppLanguage.English)
        assertEquals(SettingsSection.System, viewModel.uiState.value.section)
        assertTrue(viewModel.uiState.value.focusLanguage)

        viewModel.onSectionFocused(SettingsSection.System)
        assertFalse(viewModel.uiState.value.focusLanguage)
    }

    @Test
    fun `speed and memory options load with their defaults and save`() = runTest(mainRule.dispatcher) {
        val optimization = FakeOptimization()
        val viewModel = SettingsViewModel(FakeSettings(), FakeAppearance(), FakeLanguages(), optimization, bigDevice)
        backgroundScope.launch { viewModel.uiState.collect {} }

        assertEquals(false, viewModel.uiState.value.warmTwitchPlayer)
        assertEquals(true, viewModel.uiState.value.freeMemoryInBackground)

        viewModel.onWarmTwitchPlayerChanged(true)
        viewModel.onFreeMemoryInBackgroundChanged(false)
        assertEquals(true, viewModel.uiState.value.warmTwitchPlayer)
        assertEquals(false, viewModel.uiState.value.freeMemoryInBackground)
    }
}
