package app.twicast.tv.feature.player.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader

private const val ChatPageUrl = "https://${WebViewAssetLoader.DEFAULT_DOMAIN}/assets/player/chat.html"

/**
 * Read-only chat next to the TwiCast player (mockup "Плеер · чат"): the official chat embed in its own
 * WebView. Created when shown and destroyed when hidden, so a closed chat costs nothing.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChatPanel(channelLogin: String, title: String, hint: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                .build()
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundColor(Color.WHITE)
                isFocusable = false
                isFocusableInTouchMode = false
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                        assetLoader.shouldInterceptRequest(request.url)

                    // Links in the chat never navigate the panel away.
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                        request.isForMainFrame && request.url.host != WebViewAssetLoader.DEFAULT_DOMAIN

                    // A dead renderer only takes the chat down, never the app.
                    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean = true
                }
                loadUrl(
                    Uri.parse(ChatPageUrl).buildUpon()
                        .appendQueryParameter("channel", channelLogin)
                        .appendQueryParameter("title", title)
                        .appendQueryParameter("hint", hint)
                        .build()
                        .toString(),
                )
            }
        },
        onRelease = { webView -> webView.destroy() },
    )
}
