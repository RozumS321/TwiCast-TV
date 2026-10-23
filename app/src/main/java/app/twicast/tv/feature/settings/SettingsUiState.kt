package app.twicast.tv.feature.settings

import androidx.compose.runtime.Immutable
import app.twicast.tv.data.model.AppLanguage
import app.twicast.tv.data.model.PlayerEngine
import app.twicast.tv.data.model.ThemeMode
import app.twicast.tv.data.repository.qualityCeiling

/** Settings chips (mockup "Качество по умолчанию"): a ceiling for new streams, "Авто" lets the player adapt. */
enum class DefaultQuality(val id: String, val height: Int?) {
    Auto("auto", null),
    P1080("1080p", 1080),
    P720("720p", 720),
    P480("480p", 480),
    P360("360p", 360),
    ;

    companion object {
        /** A choice from the player ("720p60") lights the chip of its height; unknown ones light nothing. */
        fun of(storedQualityId: String?): DefaultQuality? {
            if (storedQualityId == null || storedQualityId == Auto.id) return Auto
            val height = qualityCeiling(storedQualityId) ?: storedQualityId.substringBefore('p').toIntOrNull()
            return entries.firstOrNull { it.height != null && it.height == height }
        }
    }
}

/** Left menu of Settings (mockups "Настройки · Плеер" / "Настройки · Система"). */
enum class SettingsSection { Player, System }

/** Mockup "Настройки". Choices read from storage are null until loaded: nothing is highlighted yet. */
@Immutable
data class SettingsUiState(
    /** False until the stored choices are read. */
    val loaded: Boolean = false,
    val section: SettingsSection = SettingsSection.Player,
    val playerEngine: PlayerEngine? = null,
    val defaultQuality: DefaultQuality? = null,
    val themeMode: ThemeMode? = null,
    val openChatWithStream: Boolean? = null,
    /** Read synchronously (it is what the screen is drawn in), so never null. */
    val language: AppLanguage,
    /** The screen was just redrawn in a new language: the focus goes back to the language chips. */
    val focusLanguage: Boolean = false,
)
