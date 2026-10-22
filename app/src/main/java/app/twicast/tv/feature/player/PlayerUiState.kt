package app.twicast.tv.feature.player

import androidx.compose.runtime.Immutable
import app.twicast.tv.feature.player.components.VideoQuality
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

enum class PlaybackStatus { Loading, Playing, Paused, Offline, Error }

/** What is drawn over the video (mockups "Плеер · панель управления" / "Плеер · качество"). */
enum class PlayerOverlay { Hidden, Controls, Quality }

@Immutable
data class PlayerUiState(
    val channel: PlayerDestination,
    val status: PlaybackStatus = PlaybackStatus.Loading,
    val overlay: PlayerOverlay = PlayerOverlay.Hidden,
    val chatVisible: Boolean = false,
    val qualities: ImmutableList<VideoQuality> = persistentListOf(),
    val currentQualityId: String? = null,
    /** Read from settings before the embed is created: it goes into the embed URL. */
    val preferredQualityId: String? = null,
    val settingsLoaded: Boolean = false,
) {
    /** False for a malformed login: the embed is not created at all. */
    val canPlay: Boolean get() = channel.channelLogin.matches(ChannelLoginPattern)

    /** Null for automatic quality (shown as "Авто"). */
    val currentQualityLabel: String? get() = qualities.firstOrNull { it.id == currentQualityId }?.takeUnless { it.isAuto }?.label

    private companion object {
        /** Twitch logins: 1–25 of [a-z0-9_]. Anything else never reaches the embed URL. */
        val ChannelLoginPattern = Regex("^[a-zA-Z0-9_]{1,25}$")
    }
}
