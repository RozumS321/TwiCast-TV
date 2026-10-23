package app.twicast.tv.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twicast.tv.data.model.AppLanguage
import app.twicast.tv.data.model.PlayerEngine
import app.twicast.tv.data.model.ThemeMode
import app.twicast.tv.data.repository.AppearanceSettingsRepository
import app.twicast.tv.data.repository.LanguageSettingsRepository
import app.twicast.tv.data.repository.PlayerSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data object SettingsDestination

private const val StopTimeoutMillis = 5_000L

/** Mockup "Настройки · плеер и оформление". Each choice is null until read (nothing is highlighted yet). */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: PlayerSettingsRepository,
    private val appearance: AppearanceSettingsRepository,
    private val languages: LanguageSettingsRepository,
) : ViewModel() {

    private val _language = MutableStateFlow(languages.language())

    /** Read synchronously (it is what the screen is drawn in), so never null unlike the other choices. */
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    val playerEngine: StateFlow<PlayerEngine?> =
        settings.playerEngine.stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), null)

    val themeMode: StateFlow<ThemeMode?> =
        appearance.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), null)

    fun onPlayerSelected(engine: PlayerEngine) {
        viewModelScope.launch { settings.setPlayerEngine(engine) }
    }

    fun onThemeSelected(mode: ThemeMode) {
        viewModelScope.launch { appearance.setThemeMode(mode) }
    }

    /** Saves a new language; true when it changed and the screen must be recreated to speak it. */
    fun onLanguageSelected(language: AppLanguage): Boolean {
        if (language == _language.value) return false
        languages.setLanguage(language)
        _language.value = language
        return true
    }
}
