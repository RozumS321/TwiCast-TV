package app.twicast.tv.feature.player.components

import android.graphics.Color
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsManifest
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlin.math.roundToInt

private const val HttpNotFound = 404
private const val LiveSegmentTitle = "live"

/** One selectable video track: what the quality menu shows and how to force it. */
private class QualityOption(val quality: VideoQuality, val override: TrackSelectionOverride)

/** Drives ExoPlayer for the screen; the same commands as the embed, so the screen does not care which is on. */
class NativePlayerController : PlaybackController {
    private var player: ExoPlayer? = null
    private var options: List<QualityOption> = emptyList()
    private var onQualitiesChanged: (() -> Unit)? = null

    internal fun attach(player: ExoPlayer, onQualitiesChanged: () -> Unit) {
        this.player = player
        this.onQualitiesChanged = onQualitiesChanged
    }

    internal fun detach() {
        player = null
        options = emptyList()
        onQualitiesChanged = null
    }

    override fun togglePause() {
        val player = player ?: return
        if (player.playWhenReady) player.pause() else player.play()
    }

    override fun setQuality(id: String) {
        val player = player ?: return
        val parameters = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            // The viewer's own choice lifts the low-memory ceiling.
            .clearVideoSizeConstraints()
        options.firstOrNull { it.quality.id == id }?.let { parameters.setOverrideForType(it.override) }
        player.trackSelectionParameters = parameters.build()
        onQualitiesChanged?.invoke()
    }

    override fun setChatVisible(visible: Boolean, title: String, hint: String) = Unit

    internal fun updateOptions(tracks: Tracks) {
        options = tracks.groups
            .filter { it.type == C.TRACK_TYPE_VIDEO }
            .flatMap { group ->
                (0 until group.length).mapNotNull { index ->
                    val format = group.getTrackFormat(index)
                    if (format.height <= 0) return@mapNotNull null
                    val fps = format.frameRate.takeIf { it > 0 }?.roundToInt()
                    // Twitch's own names: "720p60", "480p30".
                    val name = "${format.height}p${fps ?: ""}"
                    QualityOption(VideoQuality(name, name), TrackSelectionOverride(group.mediaTrackGroup, index))
                }
            }
            .distinctBy { it.quality.id }
            .sortedByDescending { it.quality.id.substringBefore('p').toIntOrNull() ?: 0 }
    }

    /** "auto" first, then the stream's qualities from the best down. */
    internal fun qualities(): List<VideoQuality> = listOf(VideoQuality(AutoQualityId, AutoQualityId)) + options.map { it.quality }

    internal fun currentQualityId(): String {
        val overrides = player?.trackSelectionParameters?.overrides?.values.orEmpty()
        return options.firstOrNull { option -> option.override in overrides }?.quality?.id ?: AutoQualityId
    }

    /** First tracks of a stream: the remembered quality if this stream has it, else auto within [maxVideoHeight]. */
    internal fun applyInitialQuality(preferredQualityId: String?, maxVideoHeight: Int?) {
        val player = player ?: return
        val preferred = options.firstOrNull { it.quality.id == preferredQualityId }
        val parameters = player.trackSelectionParameters.buildUpon()
        when {
            preferred != null -> parameters.setOverrideForType(preferred.override)
            maxVideoHeight != null -> parameters.setMaxVideoSize(Int.MAX_VALUE, maxVideoHeight)
            else -> return
        }
        player.trackSelectionParameters = parameters.build()
    }
}

/**
 * The TwiCast player: ExoPlayer on the stream's own HLS playlist (or a clip's MP4). No web page, so it starts
 * fast and stays light on weak TV boxes. Reports the same events as the embed through [listener].
 */
@OptIn(UnstableApi::class)
@Composable
fun NativeStreamView(
    url: String,
    isLive: Boolean,
    isHls: Boolean,
    preferredQualityId: String?,
    maxVideoHeight: Int?,
    controller: NativePlayerController,
    listener: TwitchEmbedListener,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentListener = rememberUpdatedState(listener)
    val player = remember(url) { ExoPlayer.Builder(context).build() }

    DisposableEffect(player) {
        var initialQualityApplied = false
        var adBreak = false
        fun reportQualities() = currentListener.value.onQualities(controller.qualities(), controller.currentQualityId())
        val callbacks = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                controller.updateOptions(tracks)
                if (!initialQualityApplied) {
                    initialQualityApplied = true
                    controller.applyInitialQuality(preferredQualityId, maxVideoHeight)
                }
                reportQualities()
            }

            // Twitch titles live segments "live"; anything else at the live edge is an ad (or its slate).
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (!isLive) return
                val title = (player.currentManifest as? HlsManifest)?.mediaPlaylist?.segments?.lastOrNull()?.title ?: return
                val nowAd = title.isNotEmpty() && !title.startsWith(LiveSegmentTitle)
                if (nowAd != adBreak) {
                    adBreak = nowAd
                    currentListener.value.onAdBreak(nowAd)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) Log.i(PerfTag, "native playing")
                if (isPlaying) currentListener.value.onStateChanged(EmbedState.Playing)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) currentListener.value.onStateChanged(EmbedState.Paused)
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> currentListener.value.onStateChanged(EmbedState.Ready)
                    // A live stream ends when the broadcast does; a recording just stops at its end.
                    Player.STATE_ENDED -> currentListener.value.onStateChanged(if (isLive) EmbedState.Offline else EmbedState.Paused)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.w(PerfTag, "native player error ${error.errorCodeName}", error)
                when {
                    // Fell too far behind the live edge (a long pause): jump back to live.
                    error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> {
                        player.seekToDefaultPosition()
                        player.prepare()
                    }
                    // Usher has no playlist for a channel that is not broadcasting.
                    isLive && (error.cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode == HttpNotFound ->
                        currentListener.value.onStateChanged(EmbedState.Offline)
                    else -> currentListener.value.onStateChanged(EmbedState.Error)
                }
            }
        }
        player.addListener(callbacks)
        controller.attach(player, ::reportQualities)
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .apply { if (isHls) setMimeType(MimeTypes.APPLICATION_M3U8) }
            .build()
        Log.i(PerfTag, "native start")
        player.setMediaItem(mediaItem)
        player.playWhenReady = true
        player.prepare()
        onDispose {
            controller.detach()
            player.removeListener(callbacks)
            player.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                // Our own controls: the remote is handled in Compose.
                useController = false
                isFocusable = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShutterBackgroundColor(Color.BLACK)
                keepScreenOn = true
            }
        },
        update = { view -> view.player = player },
        onRelease = { view -> view.player = null },
    )
}
