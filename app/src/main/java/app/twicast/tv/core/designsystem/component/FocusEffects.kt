package app.twicast.tv.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.twicast.tv.core.designsystem.theme.TwiMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Focus animation progress (0 = rest, 1 = focused) driven from onFocusChanged.
 * It never triggers recomposition: [value] is meant to be read only inside draw-phase lambdas.
 */
@Stable
class FocusProgress internal constructor(private val scope: CoroutineScope) {
    private val animatable = Animatable(0f)

    val value: Float get() = animatable.value

    internal fun animateTo(isFocused: Boolean) {
        scope.launch { animatable.animateTo(if (isFocused) 1f else 0f, TwiMotion.focus()) }
    }
}

@Composable
fun rememberFocusProgress(): FocusProgress {
    val scope = rememberCoroutineScope()
    return remember(scope) { FocusProgress(scope) }
}

/** Feeds [progress] from the focus state of the focusable placed after this modifier. */
fun Modifier.trackFocus(progress: FocusProgress): Modifier =
    onFocusChanged { progress.animateTo(it.isFocused) }

/**
 * Focused look shared by cards and buttons: scale and an optional colored shadow, all in one graphics layer.
 * Shadow is drawn by the RenderNode (no blur): [restShadowElevation] at rest, growing to [shadowElevation] with focus.
 */
fun Modifier.focusTransform(
    progress: FocusProgress,
    focusedScale: Float,
    shape: Shape,
    shadowElevation: Dp? = null,
    shadowColor: Color = Color.Black,
    restShadowElevation: Dp = 0.dp,
    restShadowColor: Color = shadowColor,
): Modifier = graphicsLayer {
    val fraction = progress.value
    val scale = 1f + (focusedScale - 1f) * fraction
    scaleX = scale
    scaleY = scale
    if (shadowElevation != null) {
        this.shape = shape
        val rest = restShadowElevation.toPx()
        this.shadowElevation = rest + (shadowElevation.toPx() - rest) * fraction
        val color = lerp(restShadowColor, shadowColor, fraction)
        ambientShadowColor = color
        spotShadowColor = color
    }
}

/** Fills the shape with [color] fading in with focus. */
fun Modifier.focusBackground(progress: FocusProgress, color: Color, shape: Shape): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    onDrawBehind {
        val fraction = progress.value
        if (fraction > 0f) drawOutline(outline, color, alpha = fraction)
    }
}

/** Ring of [width] drawn just outside the bounds (like CSS `box-shadow: 0 0 0 3px`), fading in with focus. */
fun Modifier.focusRing(progress: FocusProgress, color: Color, shape: Shape, width: Dp): Modifier = drawWithCache {
    val strokeWidth = width.toPx()
    val ringSize = Size(size.width + strokeWidth, size.height + strokeWidth)
    val outline = shape.createOutline(ringSize, layoutDirection, this)
    val stroke = Stroke(strokeWidth)
    val shift = -strokeWidth / 2f
    onDrawWithContent {
        drawContent()
        val fraction = progress.value
        if (fraction > 0f) {
            translate(shift, shift) { drawOutline(outline, color, alpha = fraction, style = stroke) }
        }
    }
}

/** Focuses this element once it enters composition — the entry point of a screen. Also collapses the sidebar. */
@Composable
fun Modifier.requestInitialFocus(): Modifier {
    val requester = remember { FocusRequester() }
    LaunchedEffect(requester) { requester.requestFocus() }
    return focusRequester(requester)
}
