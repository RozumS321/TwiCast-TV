package app.twicast.tv.feature.player.components

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import androidx.core.view.doOnAttach
import androidx.core.view.doOnNextLayout
import androidx.compose.runtime.staticCompositionLocalOf
import app.twicast.tv.data.model.PlayerEngine
import app.twicast.tv.data.repository.PlayerSettingsRepository
import kotlinx.coroutines.flow.first
import org.json.JSONObject

/** The activity's warm player, or null where it is not used (low-memory TVs). */
val LocalWarmTwitchPlayer = staticCompositionLocalOf<WarmTwitchPlayer?> { null }

/**
 * One Twitch player page kept loaded between streams. Loading our page, Twitch's scripts and its player takes
 * ~10 s on a TV box; already loaded, opening a stream is only `setChannel`. Started from Home with a channel the
 * viewer is likely to open, muted and without autoplay, so it downloads no video while it waits.
 *
 * The price is the WebView renderer (~250 MB) for as long as the app runs, so only with the Twitch player chosen
 * and never on low-memory TVs. Ads are not affected: Twitch decides on them when a channel starts playing.
 */
class WarmTwitchPlayer(
    private val context: Context,
    private val settings: PlayerSettingsRepository,
) {
    private var webView: WebView? = null
    private var pageLoaded = false

    /** A command sent before the page could take it; run once it has loaded. */
    private var pendingScript: String? = null

    /** The player screen using the page right now; null while it is parked. */
    private var listener: TwitchEmbedListener? = null

    private val events = object : TwitchEmbedListener {
        override fun onStateChanged(state: EmbedState) {
            listener?.onStateChanged(state)
        }

        override fun onQualities(qualities: List<VideoQuality>, currentId: String) {
            listener?.onQualities(qualities, currentId)
        }

        override fun onPageLoaded() {
            pageLoaded = true
            pendingScript?.let(::run)
            pendingScript = null
        }

        // A dead renderer cannot be reused: drop the page; the screen (if any) rebuilds the player as before.
        override fun onRendererGone() {
            val screen = listener
            destroy()
            screen?.onRendererGone()
        }
    }

    /** Loads the page in the background if the Twitch player is chosen; a no-op once it is there. */
    suspend fun prepare(channelLogin: String) {
        if (webView != null || settings.playerEngine.first() != PlayerEngine.Twitch) return
        Log.i(PerfTag, "warm player: preparing")
        create(channelLogin)
    }

    /** The player screen takes the page and switches it to its stream. */
    internal fun acquire(
        listener: TwitchEmbedListener,
        channelLogin: String,
        videoId: String?,
        preferredQualityId: String?,
        maxVideoHeight: Int?,
    ): WebView {
        val view = webView ?: create(channelLogin)
        (view.parent as? ViewGroup)?.removeView(view)
        view.alpha = 1f
        view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        this.listener = listener
        view.onResume()
        val options = JSONObject()
            .put("channel", channelLogin)
            .put("video", videoId ?: JSONObject.NULL)
            .put("quality", preferredQualityId ?: JSONObject.NULL)
            .put("maxHeight", maxVideoHeight ?: 0)
        // Twitch starts playback only in a visible, full-size player ("Autoplay disabled ... style visibility, size,
        // viewport visibility"): send the stream once the screen has attached and laid out the WebView.
        view.doOnAttach { attached ->
            attached.doOnNextLayout {
                Log.i(PerfTag, "warm player: load (page ready: $pageLoaded)")
                if (this.listener === listener) run("twicast.load($options)")
            }
        }
        return view
    }

    internal fun owns(view: WebView): Boolean = view === webView

    /** The player screen is gone: stop, mute and idle the page until the next stream. */
    internal fun release() {
        listener = null
        run("twicast.park()")
        webView?.onPause()
    }

    /** Frees the page (activity gone, or the TwiCast player chosen). */
    fun destroy() {
        webView?.let { (it.parent as? ViewGroup)?.removeView(it) }
        webView?.let(::destroyEmbedWebView)
        webView = null
        pageLoaded = false
        pendingScript = null
        listener = null
    }

    private fun create(channelLogin: String): WebView =
        createEmbedWebView(context, events).also { view ->
            webView = view
            pageLoaded = false
            // Twitch decides on autoplay when its player starts: it must be in a window, full size and visible
            // (a WebView in no window, or 1 px, gets "Autoplay disabled" and then never starts that channel).
            // So the page waits full screen in the activity's window, transparent and under the app's own views.
            (context as? Activity)?.window?.decorView?.let { it as? ViewGroup }?.addView(
                view.apply { alpha = 0f },
                0,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
            view.loadUrl(playerUrl(channelLogin, videoId = null, clipSlug = null, preferredQualityId = null, maxVideoHeight = null, warm = true))
        }

    private fun run(script: String) {
        val view = webView ?: return
        if (pageLoaded) view.evaluateJavascript(script, null) else pendingScript = script
    }
}
