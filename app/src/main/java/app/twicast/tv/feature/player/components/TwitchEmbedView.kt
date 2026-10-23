package app.twicast.tv.feature.player.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.json.JSONObject

private const val AssetHost = WebViewAssetLoader.DEFAULT_DOMAIN
private const val PlayerPageUrl = "https://$AssetHost/assets/player/player.html"
private const val BridgeName = "TwiCast"

/** Player start timings in logcat (`adb logcat -s TwiCastPerf`): WebView creation, page load, Twitch events. */
internal const val PerfTag = "TwiCastPerf"

private const val MillisPerSecond = 1000.0

private const val RendererGoneTag = "renderer-gone"

/**
 * Events reported by the embed page (see assets/player/player.html) and the TwiCast player.
 * [Buffering]: playback ran dry after it had started (TwiCast player only).
 */
enum class EmbedState { Ready, Playing, Paused, Buffering, Offline, Error }

/** One entry of the player's quality list; [id] is what setQuality() takes (Twitch "group"). */
@Immutable
@Serializable
data class VideoQuality(val id: String, val label: String) {
    /** Twitch names it "auto"; the UI shows a localized label instead. */
    val isAuto: Boolean get() = id == AutoQualityId
}

const val AutoQualityId = "auto"

/** Handle for the screen to control playback without touching the WebView. */
class TwitchEmbedController internal constructor() : PlaybackController {
    internal var webView: WebView? = null

    override fun togglePause() = call("window.twicast && twicast.togglePause()")

    override fun setQuality(id: String) = call("window.twicast && twicast.setQuality(${JSONObject.quote(id)})")

    override fun seekTo(positionMs: Long) = call("window.twicast && twicast.seek(${positionMs / MillisPerSecond})")

    override fun setChatVisible(visible: Boolean, title: String, hint: String) =
        call("window.twicast && twicast.setChatVisible($visible, ${JSONObject.quote(title)}, ${JSONObject.quote(hint)})")

    /** The app went to the background: pause the video, then let the WebView idle (timers, rendering). */
    internal fun background() {
        call("window.twicast && twicast.background && twicast.background()")
        liveWebView()?.onPause()
    }

    internal fun foreground() {
        liveWebView()?.onResume()
        call("window.twicast && twicast.foreground && twicast.foreground()")
    }

    private fun call(script: String) {
        liveWebView()?.evaluateJavascript(script, null)
    }

    // A WebView whose renderer died accepts nothing but destroy() until the screen replaces it.
    private fun liveWebView(): WebView? = webView?.takeIf { it.tag != RendererGoneTag }
}

/** Callbacks from the page, delivered on the main thread. */
interface TwitchEmbedListener {
    fun onStateChanged(state: EmbedState)
    fun onQualities(qualities: List<VideoQuality>, currentId: String)

    /** Recordings, about once a second: where playback is and how long the recording is. */
    fun onProgress(positionMs: Long, durationMs: Long) = Unit

    /** A Twitch ad break started or ended (TwiCast player): Twitch shows its own slate meanwhile. */
    fun onAdBreak(active: Boolean) = Unit

    /** The page's renderer process died (out of memory on weak boxes); this WebView is dead and gets replaced. */
    fun onRendererGone()

    /** Our wrapper page finished loading: `window.twicast` exists from now on. */
    fun onPageLoaded() = Unit
}

/**
 * The official Twitch embed in a WebView. With a [WarmTwitchPlayer] the already loaded page is borrowed and only
 * switched to this stream, and given back (parked) on exit; otherwise one WebView per player screen, created
 * when the screen enters composition and destroyed on exit. Clips always get their own (no JS API to switch to).
 * The WebView never takes focus — the remote is handled in Compose and forwarded via [TwitchEmbedController].
 */
@Composable
fun TwitchEmbedView(
    channelLogin: String,
    videoId: String?,
    clipSlug: String?,
    preferredQualityId: String?,
    maxVideoHeight: Int?,
    controller: TwitchEmbedController,
    listener: TwitchEmbedListener,
    modifier: Modifier = Modifier,
) {
    val currentListener = rememberUpdatedState(listener)
    val warmPlayer = LocalWarmTwitchPlayer.current.takeIf { clipSlug == null }
    BackgroundEffect(key = controller, onBackground = controller::background, onForeground = controller::foreground)
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val forwarding = object : TwitchEmbedListener {
                override fun onStateChanged(state: EmbedState) = currentListener.value.onStateChanged(state)
                override fun onQualities(qualities: List<VideoQuality>, currentId: String) =
                    currentListener.value.onQualities(qualities, currentId)
                override fun onRendererGone() = currentListener.value.onRendererGone()
                override fun onProgress(positionMs: Long, durationMs: Long) =
                    currentListener.value.onProgress(positionMs, durationMs)
            }
            if (warmPlayer != null) {
                warmPlayer.acquire(forwarding, channelLogin, videoId, preferredQualityId, maxVideoHeight)
                    .also { controller.webView = it }
            } else {
                val startedAt = SystemClock.elapsedRealtime()
                createEmbedWebView(context, forwarding).also { webView ->
                    Log.i(PerfTag, "webview created in ${SystemClock.elapsedRealtime() - startedAt} ms")
                    controller.webView = webView
                    webView.loadUrl(playerUrl(channelLogin, videoId, clipSlug, preferredQualityId, maxVideoHeight))
                }
            }
        },
        onRelease = { webView ->
            controller.webView = null
            if (warmPlayer != null && warmPlayer.owns(webView)) warmPlayer.release() else destroyEmbedWebView(webView)
        },
    )
}

internal fun destroyEmbedWebView(webView: WebView) {
    // A WebView whose renderer died accepts nothing but destroy().
    if (webView.tag != RendererGoneTag) {
        webView.stopLoading()
        webView.loadUrl("about:blank")
    }
    webView.removeJavascriptInterface(BridgeName)
    webView.destroy()
}

internal fun playerUrl(
    channelLogin: String,
    videoId: String?,
    clipSlug: String?,
    preferredQualityId: String?,
    maxVideoHeight: Int?,
    warm: Boolean = false,
): String =
    Uri.parse(PlayerPageUrl).buildUpon()
        .appendQueryParameter("channel", channelLogin)
        .apply { if (warm) appendQueryParameter("warm", "1") }
        .apply { if (videoId != null) appendQueryParameter("video", videoId) }
        .apply { if (clipSlug != null) appendQueryParameter("clip", clipSlug) }
        .apply { if (preferredQualityId != null) appendQueryParameter("quality", preferredQualityId) }
        .apply { if (maxVideoHeight != null) appendQueryParameter("maxHeight", maxVideoHeight.toString()) }
        .build()
        .toString()

@SuppressLint("SetJavaScriptEnabled")
internal fun createEmbedWebView(context: Context, listener: TwitchEmbedListener): WebView {
    val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setBackgroundColor(Color.BLACK)
        isFocusable = false
        isFocusableInTouchMode = false
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        // Autoplay with sound: there is no tap on a TV.
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        // Player and chat are twitch.tv iframes inside our page: they need their own cookies.
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        addJavascriptInterface(EmbedBridge(listener), BridgeName)
        webViewClient = EmbedWebViewClient(assetLoader, listener)
    }
}

/** JS → Kotlin. Called on a WebView binder thread, so everything is posted to the main thread. */
private class EmbedBridge(private val listener: TwitchEmbedListener) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val json = Json { ignoreUnknownKeys = true }

    @JavascriptInterface
    fun onStateChanged(state: String) {
        val embedState = when (state) {
            "ready" -> EmbedState.Ready
            "playing" -> EmbedState.Playing
            "paused" -> EmbedState.Paused
            "offline" -> EmbedState.Offline
            else -> EmbedState.Error
        }
        mainHandler.post { listener.onStateChanged(embedState) }
    }

    /** Timing marks from the page (ms since its navigation start). */
    @JavascriptInterface
    fun mark(label: String, atMillis: Int) {
        Log.i(PerfTag, "page +$atMillis ms: $label")
    }

    /** Seconds from the page (Twitch's getCurrentTime / getDuration). */
    @JavascriptInterface
    fun onProgress(positionSeconds: Double, durationSeconds: Double) {
        val position = (positionSeconds * MillisPerSecond).toLong()
        val duration = (durationSeconds * MillisPerSecond).toLong()
        mainHandler.post { listener.onProgress(position, duration) }
    }

    @JavascriptInterface
    fun onQualities(qualitiesJson: String, currentId: String) {
        val qualities = try {
            json.decodeFromString<List<VideoQuality>>(qualitiesJson)
        } catch (error: SerializationException) {
            // Unknown shape from a future player version: no menu entries rather than a crash.
            emptyList()
        }
        mainHandler.post { listener.onQualities(qualities, currentId) }
    }
}

private class EmbedWebViewClient(
    private val assetLoader: WebViewAssetLoader,
    private val listener: TwitchEmbedListener,
) : WebViewClient() {

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
        assetLoader.shouldInterceptRequest(request.url)

    /** The top-level page never leaves our wrapper (links inside the player must not open the Twitch site here). */
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        request.isForMainFrame && request.url.host != AssetHost

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        if (url != "about:blank") Log.i(PerfTag, "page started")
    }

    override fun onPageFinished(view: WebView, url: String) {
        if (url == "about:blank") return
        Log.i(PerfTag, "page finished")
        listener.onPageLoaded()
    }

    /** Without this the whole app is killed along with the renderer. */
    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        Log.w(PerfTag, "renderer gone, crashed=${detail.didCrash()}")
        view.tag = RendererGoneTag
        listener.onRendererGone()
        return true
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame) listener.onStateChanged(EmbedState.Error)
    }
}
