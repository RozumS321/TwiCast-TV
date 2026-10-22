package app.twicast.tv.feature.player.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
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

/** Events reported by the embed page (see assets/player/player.html). */
enum class EmbedState { Ready, Playing, Paused, Offline, Error }

/** One entry of the player's quality list; [id] is what setQuality() takes (Twitch "group"). */
@Immutable
@Serializable
data class VideoQuality(val id: String, val label: String) {
    /** Twitch names it "auto"; the UI shows a localized label instead. */
    val isAuto: Boolean get() = id == AutoQualityId
}

const val AutoQualityId = "auto"

/** Handle for the screen to control playback without touching the WebView. */
class TwitchEmbedController internal constructor() {
    internal var webView: WebView? = null

    fun togglePause() = call("window.twicast && twicast.togglePause()")

    fun setQuality(id: String) = call("window.twicast && twicast.setQuality(${JSONObject.quote(id)})")

    fun setChatVisible(visible: Boolean, title: String, hint: String) =
        call("window.twicast && twicast.setChatVisible($visible, ${JSONObject.quote(title)}, ${JSONObject.quote(hint)})")

    private fun call(script: String) {
        webView?.evaluateJavascript(script, null)
    }
}

/** Callbacks from the page, delivered on the main thread. */
interface TwitchEmbedListener {
    fun onStateChanged(state: EmbedState)
    fun onQualities(qualities: List<VideoQuality>, currentId: String)
}

/**
 * The official Twitch embed in a WebView. One instance per player screen: created when the screen enters
 * composition and destroyed on exit (no leaked WebView or Activity context). The WebView never takes focus —
 * the remote is handled in Compose and forwarded via [TwitchEmbedController].
 */
@Composable
fun TwitchEmbedView(
    channelLogin: String,
    videoId: String?,
    clipSlug: String?,
    preferredQualityId: String?,
    controller: TwitchEmbedController,
    listener: TwitchEmbedListener,
    modifier: Modifier = Modifier,
) {
    val currentListener = rememberUpdatedState(listener)
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val forwarding = object : TwitchEmbedListener {
                override fun onStateChanged(state: EmbedState) = currentListener.value.onStateChanged(state)
                override fun onQualities(qualities: List<VideoQuality>, currentId: String) =
                    currentListener.value.onQualities(qualities, currentId)
            }
            createEmbedWebView(context, forwarding).also { webView ->
                controller.webView = webView
                webView.loadUrl(playerUrl(channelLogin, videoId, clipSlug, preferredQualityId))
            }
        },
        onRelease = { webView ->
            controller.webView = null
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.removeJavascriptInterface(BridgeName)
            webView.destroy()
        },
    )
}

private fun playerUrl(channelLogin: String, videoId: String?, clipSlug: String?, preferredQualityId: String?): String =
    Uri.parse(PlayerPageUrl).buildUpon()
        .appendQueryParameter("channel", channelLogin)
        .apply { if (videoId != null) appendQueryParameter("video", videoId) }
        .apply { if (clipSlug != null) appendQueryParameter("clip", clipSlug) }
        .apply { if (preferredQualityId != null) appendQueryParameter("quality", preferredQualityId) }
        .build()
        .toString()

@SuppressLint("SetJavaScriptEnabled")
private fun createEmbedWebView(context: Context, listener: TwitchEmbedListener): WebView {
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

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame) listener.onStateChanged(EmbedState.Error)
    }
}
