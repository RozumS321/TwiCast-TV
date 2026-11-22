package app.twicast.tv.core.designsystem.component

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.twicast.tv.core.designsystem.theme.TwiBackdropStyle
import app.twicast.tv.core.designsystem.theme.TwiEffectsTokens
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.core.designsystem.theme.TwiThemeConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.random.Random

private const val GrainSize = 192

/**
 * The bloom moves slowly, so it is redrawn only this many times per second (out of 60): in between nothing on
 * screen changes and the TV draws nothing at all. Smooth enough for a ten-second breath, light on weak GPUs.
 */
private const val BloomFramesPerSecond = 12

/** How far and how much the bloom breathes over one loop. */
private const val RiseFraction = 0.12f
private const val GrowFraction = 0.18f
private const val MinBloomAlpha = 0.7f

/** The frame glass shows under itself: small (the light has no detail to lose) and blurred like frosted glass. */
private const val BackdropWidthPx = 320
private const val BackdropBlurRadius = 10
private const val BackdropBrighten = 1.2f

/**
 * The living background shared by every screen: its animation, and the same frame small and blurred, so each glass
 * surface can draw the piece behind it — frosted glass without real blur (Android 11 has none). Computed only
 * while the background is seen: paused under the player and while the app is in the background.
 */
@Stable
class EmberBackdrop internal constructor(internal val steps: Int) {
    internal val phase = Animatable(0f)
    internal val step: State<Int> = derivedStateOf { (phase.value * steps).toInt() }
    internal var started = false

    /** The background in pixels (the whole app): glass maps its own position onto [blurred] with it. */
    var size by mutableStateOf(IntSize.Zero)
        internal set

    /** Storm: the current clouds, small, drawn on screen scaled up with smoothing. */
    var sharp by mutableStateOf<ImageBitmap?>(null)
        internal set

    /** The current frame, small and blurred; null until the first one is ready. */
    var blurred by mutableStateOf<ImageBitmap?>(null)
        internal set

    /** A full-screen video covers the background: nothing to animate or blur. */
    var coveredByVideo by mutableStateOf(false)

    internal var appVisible by mutableStateOf(true)

    internal val running: Boolean get() = !coveredByVideo && appVisible
}

/** The background behind glass, or null without "Эффекты интерфейса". */
val LocalEmberBackdrop = staticCompositionLocalOf<EmberBackdrop?> { null }

@Composable
fun rememberEmberBackdrop(): EmberBackdrop = remember {
    EmberBackdrop(TwiThemeConfig.effects.bloomLoopMillis * BloomFramesPerSecond / MillisPerSecond)
}

/**
 * "Эффекты интерфейса": Ember Bloom — warm light rising out of the dark under film grain, and a softer light from
 * above (canvas, "UI-библиотека"). Plain radial gradients and a tiled noise bitmap: no shaders, so it runs on
 * Android 11 TVs. Animated only in the draw phase: the loop never recomposes anything.
 */
@Composable
fun EmberBackground(backdrop: EmberBackdrop, modifier: Modifier = Modifier) {
    val tokens = TwiThemeConfig.effects
    LifecycleResumeEffect(backdrop) {
        backdrop.appVisible = true
        onPauseOrDispose { backdrop.appVisible = false }
    }
    when (tokens.backdrop) {
        TwiBackdropStyle.Bloom -> BloomAnimation(tokens, backdrop)
        TwiBackdropStyle.Storm -> StormAnimation(tokens, backdrop)
    }
    val grain = remember { grainBitmap() }
    val ground = if (TwiTheme.colors.isDark) tokens.stormDark.ground else tokens.stormLight.ground
    Box(modifier.onSizeChanged { backdrop.size = it }) {
        when (tokens.backdrop) {
            TwiBackdropStyle.Bloom -> Box(
                Modifier
                    .matchParentSize()
                    // Its own layer: redrawing the light never re-records the screens above it.
                    .graphicsLayer()
                    .drawBehind { drawEmber(tokens, backdrop.step.value.toFloat() / backdrop.steps) },
            )
            TwiBackdropStyle.Storm -> Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer()
                    .drawBehind {
                        drawRect(ground)
                        backdrop.sharp?.let { frame ->
                            drawImage(frame, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = FilterQuality.Low)
                        }
                    },
            )
        }
        // Grain is drawn once into its own layer and only composited afterwards.
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer()
                .drawWithCache {
                    val grainBrush = ShaderBrush(ImageShader(grain, TileMode.Repeated, TileMode.Repeated))
                    onDrawBehind { drawRect(grainBrush, alpha = tokens.grainAlpha) }
                },
        )
    }
}

@Composable
private fun BloomAnimation(tokens: TwiEffectsTokens, backdrop: EmberBackdrop) {
    LaunchedEffect(backdrop) {
        // The first seconds after launch are the busiest (home, images, the Twitch player): an animation there
        // would stutter. The light stands still until the app has settled, then starts breathing from rest.
        if (!backdrop.started) {
            delay(StartDelayMillis)
            backdrop.started = true
        }
        while (true) {
            snapshotFlow { backdrop.running }.first { it }
            val remaining = ((1f - backdrop.phase.value) * tokens.bloomLoopMillis).toInt()
            val finished = runCatchingAnimation {
                backdrop.phase.animateTo(1f, tween(remaining, easing = LinearEasing))
            }
            if (finished) backdrop.phase.snapTo(0f)
        }
    }
    // Stops the animation as soon as the background gets covered; the loop above resumes it.
    LaunchedEffect(backdrop) {
        snapshotFlow { backdrop.running }.collectLatest { running -> if (!running) backdrop.phase.stop() }
    }
    // The blurred frame for glass: off the main thread, once per step, only while something changes.
    LaunchedEffect(backdrop) {
        snapshotFlow { backdrop.step.value to backdrop.size }.collectLatest { (step, size) ->
            if (size.width <= 0 || size.height <= 0) return@collectLatest
            backdrop.blurred = withContext(Dispatchers.Default) { renderBackdrop(tokens, step.toFloat() / backdrop.steps, size) }
        }
    }
}

/**
 * Storm: the clouds are not a loop, they keep rolling. A new frame [StormFramesPerSecond] times a second, computed
 * off the main thread (sharp for the screen, blurred for glass); paused while covered or in the background.
 */
@Composable
private fun StormAnimation(tokens: TwiEffectsTokens, backdrop: EmberBackdrop) {
    val dark = TwiTheme.colors.isDark
    val look = remember(tokens, dark) {
        val storm = if (dark) tokens.stormDark else tokens.stormLight
        StormLook(
            ground = storm.ground.toArgb(),
            cloud = storm.cloud.toArgb(),
            light = storm.light.toArgb(),
            edge = storm.edge.toArgb(),
            cover = tokens.stormCover,
            brightness = tokens.stormBrightness,
        )
    }
    LaunchedEffect(backdrop, look) {
        var seconds = StormStartSeconds
        val frameMillis = MillisPerSecond.toLong() / StormFramesPerSecond
        while (true) {
            val (sharp, blurred) = withContext(Dispatchers.Default) { renderStorm(look, seconds, if (dark) BackdropBrighten else LightStormBlurTone) }
            backdrop.sharp = sharp
            backdrop.blurred = blurred
            // The first seconds after launch are the busiest: the clouds stand still until the app has settled.
            if (!backdrop.started) {
                delay(StartDelayMillis)
                backdrop.started = true
            }
            snapshotFlow { backdrop.running }.first { it }
            delay(frameMillis)
            seconds += 1f / StormFramesPerSecond
        }
    }
}

private const val StormFramesPerSecond = 12
private const val StormStartSeconds = 4f
private const val StormBlurRadius = 5

/**
 * Glass over the light storm: the blur is toned slightly down instead of brightened (brightened, the pale clouds
 * washed out to white and glass read as a white sheet).
 */
private const val LightStormBlurTone = 0.94f

private fun renderStorm(look: StormLook, seconds: Float, brighten: Float): Pair<ImageBitmap, ImageBitmap> {
    val pixels = stormFrame(look, seconds)
    val sharp = Bitmap.createBitmap(pixels, StormWidthPx, StormHeightPx, Bitmap.Config.ARGB_8888)
    val soft = pixels.copyOf()
    frostBlur(soft, StormWidthPx, StormHeightPx, StormBlurRadius, brighten)
    val blurred = Bitmap.createBitmap(soft, StormWidthPx, StormHeightPx, Bitmap.Config.ARGB_8888)
    return sharp.asImageBitmap() to blurred.asImageBitmap()
}

/** True when the animation ran to its end, false when it was stopped because the background got covered. */
private suspend fun runCatchingAnimation(block: suspend () -> Unit): Boolean = try {
    block()
    true
} catch (stopped: CancellationException) {
    // Our own coroutine cancelled (the screen left): pass it on. Only Animatable.stop() is swallowed.
    currentCoroutineContext().ensureActive()
    false
}

private fun DrawScope.drawEmber(tokens: TwiEffectsTokens, t: Float) {
    drawRect(tokens.emberBase)
    drawBloom(tokens, wave(t), centerX = size.width * 0.45f, radius = size.height * 0.95f)
    // A smaller bloom on the right, half a loop apart: the light never pulses as one block.
    drawBloom(tokens, wave(t + 0.5f), centerX = size.width * 0.96f, radius = size.height * 0.62f, strength = 0.75f)
    // Soft warm light from above (canvas: "Главная · эффекты"): the room is lit, not only the floor.
    drawTopGlow(tokens, wave(t + 0.25f))
}

/** The same frame as on screen, drawn at 320 px wide and blurred: what frosted glass shows. */
private fun renderBackdrop(tokens: TwiEffectsTokens, t: Float, screen: IntSize): ImageBitmap {
    val width = BackdropWidthPx
    val height = (BackdropWidthPx * screen.height / screen.width.toFloat()).roundToInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val image = bitmap.asImageBitmap()
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(width.toFloat(), height.toFloat())) {
        drawEmber(tokens, t)
    }
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    frostBlur(pixels, width, height, BackdropBlurRadius, BackdropBrighten)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return image
}

private const val MillisPerSecond = 1000
private const val StartDelayMillis = 3_000L

/** How far the light from above drifts sideways over a loop, as a share of the width. */
private const val TopDrift = 0.08f
private const val TopAlpha = 0.75f

private fun DrawScope.drawTopGlow(tokens: TwiEffectsTokens, wave: Float) {
    val radius = size.height * 1.05f
    val center = Offset(size.width * (0.55f + TopDrift * wave), -size.height * 0.12f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to tokens.emberCore.copy(alpha = 0.75f),
            0.3f to tokens.emberMid.copy(alpha = 0.55f),
            0.55f to tokens.emberEdge.copy(alpha = 0.3f),
            0.8f to Color.Transparent,
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
        alpha = TopAlpha * (0.8f + 0.2f * wave),
    )
}

/** 0 → 1 → 0 over one loop, eased at both ends. */
private fun wave(t: Float): Float = ((1 - cos(2 * PI * (t % 1f))) / 2).toFloat()

private fun DrawScope.drawBloom(tokens: TwiEffectsTokens, wave: Float, centerX: Float, radius: Float, strength: Float = 1f) {
    val r = radius * (1f + GrowFraction * wave)
    val center = Offset(centerX, size.height + radius * 0.35f - size.height * RiseFraction * wave)
    val alpha = strength * (MinBloomAlpha + (1f - MinBloomAlpha) * wave)
    drawCircle(
        brush = Brush.radialGradient(
            0f to tokens.emberCore.copy(alpha = 0.95f),
            0.24f to tokens.emberMid.copy(alpha = 0.7f),
            0.46f to tokens.emberEdge.copy(alpha = 0.35f),
            0.7f to Color.Transparent,
            center = center,
            radius = r,
        ),
        radius = r,
        center = center,
        alpha = alpha,
    )
}

/** Grey noise with transparent gaps: light and dark specks over whatever is below. */
internal fun grainBitmap(): ImageBitmap {
    val random = Random(GrainSize)
    val pixels = IntArray(GrainSize * GrainSize) {
        val value = random.nextInt(256)
        val alpha = random.nextInt(140)
        (alpha shl 24) or (value shl 16) or (value shl 8) or value
    }
    return Bitmap.createBitmap(pixels, GrainSize, GrainSize, Bitmap.Config.ARGB_8888).asImageBitmap()
}
