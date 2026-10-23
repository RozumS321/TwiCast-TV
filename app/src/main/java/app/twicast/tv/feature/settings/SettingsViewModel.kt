package app.twicast.tv.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twicast.tv.data.model.AppLanguage
import app.twicast.tv.data.model.PlayerEngine
import app.twicast.tv.data.model.ThemeMode
import app.twicast.tv.data.repository.AppearanceSettingsRepository
import app.twicast.tv.core.device.DeviceProfile
import app.twicast.tv.data.repository.LanguageSettingsRepository
import app.twicast.tv.data.repository.OptimizationSettingsRepository
import app.twicast.tv.data.repository.PlayerSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data object SettingsDestination

private const val StopTimeoutMillis = 5_000L

/** Mockup "Настройки": player, default quality, chat; appearance, language; speed against memory. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: PlayerSettingsRepository,
    private val appearance: AppearanceSettingsRepository,
    private val languages: LanguageSettingsRepository,
    private val optimization: OptimizationSettingsRepository,
    device: DeviceProfile,
) : ViewModel() {

    private val lowMemory = device.isLowMemory

    private class LanguageChoice(val language: AppLanguage, val justChanged: Boolean)

    // Survives the activity being recreated for the new language, so the redrawn screen knows it was a change.
    private val languageChoice = MutableStateFlow(LanguageChoice(languages.language(), justChanged = false))

    // Also survives the recreation: a language change redraws the screen still in "Система".
    private val section = MutableStateFlow(SettingsSection.Player)

    val uiState: StateFlow<SettingsUiState> = combine(
        settings.playerEngine,
        settings.preferredQualityUpdates,
        appearance.themeMode,
        settings.openChatWithStream,
        languageChoice,
    ) { engine, quality, theme, openChat, choice ->
        SettingsUiState(
            loaded = true,
            playerEngine = engine,
            defaultQuality = DefaultQuality.of(quality),
            themeMode = theme,
            openChatWithStream = openChat,
            language = choice.language,
            focusLanguage = choice.justChanged,
        )
    }.combine(section) { state, section -> state.copy(section = section) }
        .combine(combine(optimization.warmTwitchPlayer, optimization.freeMemoryInBackground, ::Pair)) { state, (warm, free) ->
            state.copy(warmTwitchPlayer = warm, freeMemoryInBackground = free, lowMemory = lowMemory)
        }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(StopTimeoutMillis),
        SettingsUiState(language = languageChoice.value.language),
    )

    /** Sections open as soon as their menu item takes focus, as usual on TV. */
    fun onSectionFocused(focused: SettingsSection) {
        section.value = focused
        // Back in the menu after a language change: the language chips must not grab the focus again.
        val choice = languageChoice.value
        if (choice.justChanged) languageChoice.value = LanguageChoice(choice.language, justChanged = false)
    }

    fun onWarmTwitchPlayerChanged(enabled: Boolean) {
        viewModelScope.launch { optimization.setWarmTwitchPlayer(enabled) }
    }

    fun onFreeMemoryInBackgroundChanged(enabled: Boolean) {
        viewModelScope.launch { optimization.setFreeMemoryInBackground(enabled) }
    }

    fun onPlayerSelected(engine: PlayerEngine) {
        viewModelScope.launch { settings.setPlayerEngine(engine) }
    }

    /** Same storage as a choice in the player: whichever was made last applies to the next stream. */
    fun onDefaultQualitySelected(quality: DefaultQuality) {
        viewModelScope.launch { settings.setPreferredQuality(quality.id) }
    }

    fun onThemeSelected(mode: ThemeMode) {
        viewModelScope.launch { appearance.setThemeMode(mode) }
    }

    fun onOpenChatChanged(open: Boolean) {
        viewModelScope.launch { settings.setOpenChatWithStream(open) }
    }

    /** Saves a new language; true when it changed and the screen must be recreated to speak it. */
    fun onLanguageSelected(selected: AppLanguage): Boolean {
        if (selected == languageChoice.value.language) return false
        languages.setLanguage(selected)
        languageChoice.value = LanguageChoice(selected, justChanged = true)
        return true
    }
}
