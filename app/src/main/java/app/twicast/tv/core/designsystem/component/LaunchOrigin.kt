package app.twicast.tv.core.designsystem.component

import android.os.Looper
import android.os.Handler
import coil3.request.crossfade
import coil3.request.ImageRequest
import coil3.compose.LocalPlatformContext
import app.twicast.tv.core.designsystem.theme.TwiTheme
import androidx.compose.foundation.background
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import app.twicast.tv.core.designsystem.theme.TwiMotion
import app.twicast.tv.core.designsystem.theme.TwiThemeConfig
import coil3.compose.AsyncImage
import kotlin.math.roundToInt

/** A card that is opening the player: where it sat on screen and its preview. */
class Launch internal constructor(val bounds: Rect, val imageUrl: String?)

/**
 * Canvas "Плеер · открытие эфира": the preview of the card that was pressed grows from its place to the whole
 * screen, covers the player while the video starts, then fades away. Set by a card right before its click is handled.
 */
object LaunchOrigin {
    /** Centre of the last opened card as a fraction of the screen; the screen centre until a card is opened. */
    var origin: TransformOrigin = TransformOrigin.Center
        private set

    internal var pending by mutableStateOf<Launch?>(null)

    private val main = Handler(Looper.getMainLooper())

    /**
     * Grows the card at [coordinates] (still attached) into the screen, then runs [open]. Opening the player takes
     * the main thread for a few hundred milliseconds; done first it froze the growth, so the preview jumped to full
     * screen. Done after, the stall hides under the preview that already covers the screen.
     */
    fun launch(coordinates: LayoutCoordinates?, imageUrl: String?, open: () -> Unit) {
        val card = coordinates?.takeIf { it.isAttached }
        val root = card?.findRootCoordinates()?.size
        if (card == null || root == null || root.width == 0 || root.height == 0 || pending != null) {
            open()
            return
        }
        val bounds = card.boundsInRoot()
        origin = TransformOrigin(bounds.center.x / root.width, bounds.center.y / root.height)
        pending = Launch(bounds, imageUrl)
        main.postDelayed(open, TwiMotion.LaunchMillis.toLong())
    }
}

/** How long the grown preview stays over the player before it fades: the video usually starts in this time. */
private const val HoldMillis = 900
private const val FadeMillis = 350

/** Draws [LaunchOrigin]'s growing preview over the whole app. Put it last in the root box. */
@Composable
fun LaunchOverlay() {
    val launch = LaunchOrigin.pending ?: return
    val grow = remember(launch) { Animatable(0f) }
    val alpha = remember(launch) { Animatable(1f) }
    LaunchedEffect(launch) {
        grow.animateTo(1f, tween(TwiMotion.LaunchMillis, easing = TwiMotion.Decelerate))
        kotlinx.coroutines.delay(HoldMillis.toLong())
        alpha.animateTo(0f, tween(FadeMillis))
        if (LaunchOrigin.pending === launch) LaunchOrigin.pending = null
    }
    val density = LocalDensity.current
    val radius = TwiThemeConfig.radius.thumbnail
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenW = with(density) { maxWidth.toPx() }
        val screenH = with(density) { maxHeight.toPx() }
        val f = grow.value
        val left = lerp(launch.bounds.left, 0f, f)
        val top = lerp(launch.bounds.top, 0f, f)
        val width = lerp(launch.bounds.width, screenW, f)
        val height = lerp(launch.bounds.width / (16f / 9f), screenH, f)
        Box(
            Modifier
                .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .size(with(density) { width.toDp() }, with(density) { height.toDp() })
                .graphicsLayer { this.alpha = alpha.value }
                .clip(RoundedCornerShape(lerp(radius.value, 0f, f).dp))
                // Visible from the first frame even before the picture is there.
                .background(TwiTheme.colors.surfaceVariant),
        ) {
            val context = LocalPlatformContext.current
            AsyncImage(
                // Straight from the memory cache, no fade: the app-wide crossfade left it see-through while growing.
                model = remember(launch) { ImageRequest.Builder(context).data(launch.imageUrl).crossfade(false).build() },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
