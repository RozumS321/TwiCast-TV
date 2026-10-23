package app.twicast.tv.feature.player

import androidx.compose.runtime.Immutable
import app.twicast.tv.data.model.PlayerEngine
import app.twicast.tv.feature.player.components.VideoQuality
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** [Ended]: the broadcast stopped while it was being watched (not just a channel that is offline). */
enum class PlaybackStatus { Loading, Playing, Paused, Offline, Ended, Error }

/**
 * What is drawn over the video (mockups "Плеер · панель управления" / "Плеер · качество").
 * [Seek]: only the seek bar, while a recording is wound from the bare video ("Плеер · запись, перематываю").
 */
enum class PlayerOverlay { Hidden, Controls, Quality, Seek }

/** Where a recording is: for the seek bar. */
@Immutable
data class PlaybackProgress(val positionMs: Long, val durationMs: Long)

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
    /** Bumped when the WebView renderer died: the embed is rebuilt from scratch. */
    val embedAttempt: Int = 0,
    /** Which player plays; null until read from settings. */
    val engine: PlayerEngine? = null,
    /** TwiCast player: the stream's playlist (or clip file), once resolved. */
    val streamUrl: String? = null,
    /** Set after a renderer crash (out of memory on weak TV boxes): the embed keeps quality at or below it. */
    val maxVideoHeight: Int? = null,
    /** Twitch is in an ad break (TwiCast player): its slate plays, a plate explains it. */
    val adBreak: Boolean = false,
    /** Recordings: reported by the player about once a second. */
    val progress: PlaybackProgress? = null,
    /** While the viewer winds: the position being dialled, and where the winding started (for "+ 1 мин"). */
    val seekTargetMs: Long? = null,
    val seekStartMs: Long? = null,
) {
    /** Only recordings wind; a live stream and a clip don't. */
    val canSeek: Boolean get() = channel.videoId != null && progress != null

    /** What the seek bar shows: the dialled position while winding, else where playback is. */
    val shownPositionMs: Long? get() = seekTargetMs ?: progress?.positionMs

    /** "+ 1 мин" over the seek bar while winding. */
    val seekDeltaMs: Long? get() = seekTargetMs?.let { target -> seekStartMs?.let { target - it } }

    /** False for a malformed login: the embed is not created at all. */
    val canPlay: Boolean get() = channel.channelLogin.matches(ChannelLoginPattern)

    /** Null for automatic quality (shown as "Авто"). */
    val currentQualityLabel: String? get() = qualities.firstOrNull { it.id == currentQualityId }?.takeUnless { it.isAuto }?.label

    private companion object {
        /** Twitch logins: 1–25 of [a-z0-9_]. Anything else never reaches the embed URL. */
        val ChannelLoginPattern = Regex("^[a-zA-Z0-9_]{1,25}$")
    }
}
