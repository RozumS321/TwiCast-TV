package app.twicast.tv.core.designsystem.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import app.twicast.tv.core.designsystem.theme.TwiLogoTokens
import app.twicast.tv.core.designsystem.theme.TwiThemeConfig
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** The still frame: what the launcher icon, the banner and notifications show (they cannot animate). */
internal const val AuroraRestSeconds = 3f

private const val MillisPerSecond = 1000f
private const val TileCornerFraction = 0.25f

/** One soft patch of light: centre and radius in the box's units, its colour and strength. */
internal data class AuroraPatch(val center: Offset, val radius: Float, val color: Color, val alpha: Float)

/**
 * Where the three patches of light are at [seconds], inside [box] (canvas: the same loops as "Логотип · Aurora").
 * Each moves on its own slow loop, never in step with the others, so the mark never pulses as a whole.
 */
internal fun auroraPatches(tokens: TwiLogoTokens, seconds: Float, box: Rect): List<AuroraPatch> {
    val t = seconds
    val s = max(box.width, box.height)
    fun at(u: Float, v: Float) = Offset(box.left + box.width * u, box.top + box.height * v)
    return listOf(
        AuroraPatch(at(0.5f + 0.45f * sin(t * 0.47f), 0.3f + 0.35f * sin(t * 0.31f + 1f)), s * 0.55f, tokens.glowA, 0.7f * tokens.intensity),
        AuroraPatch(at(0.5f + 0.45f * sin(t * 0.37f + 2.1f), 0.7f + 0.3f * cos(t * 0.43f)), s * 0.5f, tokens.glowB, 0.6f * tokens.intensity),
        AuroraPatch(at(0.5f + 0.45f * cos(t * 0.29f + 4f), 0.5f + 0.4f * sin(t * 0.53f + 3f)), s * 0.45f, tokens.glowC, 0.55f * tokens.intensity),
    )
}

/**
 * The TwiCast mark: a dark tile with colour flowing inside the play. Plain radial gradients added together and
 * clipped to the play — no shader, so it runs on Android 11. Animated only in its own layer's draw phase, at
 * [TwiLogoTokens.framesPerSecond]; the frame clock stops it while the app is in the background.
 *
 * @param animated false shows the still frame (the same one the launcher icon and the banner use).
 */
/**
 * Seconds on the Aurora clock: ticks [TwiLogoTokens.framesPerSecond] times a second while [animated]; the frame
 * clock stops it while the app is in the background. Read it only while drawing: it never recomposes anything.
 */
@Composable
fun rememberAuroraSeconds(animated: Boolean = true): State<Float> {
    val tokens = TwiThemeConfig.logo
    val seconds = remember { mutableFloatStateOf(AuroraRestSeconds) }
    if (animated) {
        LaunchedEffect(tokens.framesPerSecond) {
            var start = -1L
            val frameMillis = (MillisPerSecond / tokens.framesPerSecond).toLong()
            while (true) {
                withFrameMillis { now ->
                    if (start < 0) start = now
                    seconds.floatValue = AuroraRestSeconds + (now - start) / MillisPerSecond
                }
                delay(frameMillis)
            }
        }
    }
    return seconds
}

/**
 * The TwiCast mark: a dark tile with colour flowing inside the play. Plain radial gradients added together and
 * clipped to the play — no shader, so it runs on Android 11. Animated only in its own layer's draw phase.
 *
 * @param seconds the Aurora clock; pass the wordmark's to keep both flowing together.
 */
@Composable
fun AuroraLogo(modifier: Modifier = Modifier, animated: Boolean = true, seconds: State<Float> = rememberAuroraSeconds(animated)) {
    val tokens = TwiThemeConfig.logo
    Spacer(
        modifier
            .graphicsLayer()
            .drawWithCache {
                val side = size.minDimension
                val play = playPath(side)
                val window = Rect(side * 0.3f, side * 0.2f, side * 0.8f, side * 0.8f)
                onDrawBehind {
                    drawRoundRect(tokens.tile, cornerRadius = CornerRadius(side * TileCornerFraction))
                    clipPath(play) { drawAurora(tokens, seconds.value, window) }
                }
            },
    )
}

/** How far the wordmark's flow runs ahead of the mark's, so the two never show the same colour at once. */
private const val WordmarkOffsetSeconds = 1.7f

/**
 * Fills whatever this draws (the "TwiCast" wordmark) with the same flowing colour as the mark: the glyphs become
 * the window. Base colour replaces the text colour (SrcIn), then the patches of light lie only on the glyphs (SrcAtop).
 */
fun Modifier.auroraText(seconds: State<Float>): Modifier = graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        val tokens = TwiThemeConfig.logo
        drawContent()
        drawRect(tokens.base, blendMode = BlendMode.SrcIn)
        val box = Rect(Offset.Zero, size)
        for (patch in auroraPatches(tokens, seconds.value + WordmarkOffsetSeconds, box)) {
            drawCircle(
                brush = Brush.radialGradient(
                    0f to patch.color.copy(alpha = patch.alpha.coerceAtMost(1f)),
                    1f to patch.color.copy(alpha = 0f),
                    center = patch.center,
                    radius = patch.radius,
                ),
                radius = patch.radius,
                center = patch.center,
                blendMode = BlendMode.SrcAtop,
            )
        }
    }

private fun DrawScope.drawAurora(tokens: TwiLogoTokens, seconds: Float, box: Rect) {
    drawRect(tokens.base, topLeft = box.topLeft, size = box.size)
    for (patch in auroraPatches(tokens, seconds, box)) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to patch.color.copy(alpha = patch.alpha.coerceAtMost(1f)),
                1f to patch.color.copy(alpha = 0f),
                center = patch.center,
                radius = patch.radius,
            ),
            radius = patch.radius,
            center = patch.center,
            // Light adds up, like on the canvas.
            blendMode = BlendMode.Plus,
        )
    }
}

/** The play, slightly right of centre like the static mark (ic_logo_mark: M17.5,13 v22 l17.5,-11 with a round 2.5 stroke). */
private fun playPath(side: Float): Path = Path().apply {
    moveTo(side * 0.34f, side * 0.245f)
    lineTo(side * 0.34f, side * 0.755f)
    lineTo(side * 0.755f, side * 0.5f)
    close()
}
