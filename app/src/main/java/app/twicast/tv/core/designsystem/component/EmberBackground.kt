package app.twicast.tv.core.designsystem.component

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import app.twicast.tv.core.designsystem.theme.TwiEffectsTokens
import app.twicast.tv.core.designsystem.theme.TwiThemeConfig
import kotlin.math.PI
import kotlin.math.cos
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

/**
 * "Эффекты интерфейса": Ember Bloom — warm light rising out of the dark under heavy film grain (canvas,
 * "UI-библиотека"). Drawn with plain radial gradients and a tiled noise bitmap, no shaders or blur, so it runs on
 * Android 11 TVs. Animated only in the draw phase: the loop never recomposes anything.
 */
@Composable
fun EmberBackground(modifier: Modifier = Modifier) {
    val tokens = TwiThemeConfig.effects
    val transition = rememberInfiniteTransition(label = "ember")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(tokens.bloomLoopMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "ember-phase",
    )
    val steps = tokens.bloomLoopMillis * BloomFramesPerSecond / MillisPerSecond
    // Changes only at each step, so the draw below is invalidated 12 times a second, not 60.
    val step = remember(phase) { derivedStateOf { (phase.value * steps).toInt() } }
    val grain = remember { grainBitmap() }
    Box(modifier) {
        Box(
            Modifier
                .matchParentSize()
                // Its own layer: redrawing the light never re-records the screens above it.
                .graphicsLayer()
                .drawBehind {
                    drawRect(tokens.emberBase)
                    val t = step.value.toFloat() / steps
                    drawBloom(tokens, wave(t), centerX = size.width * 0.45f, radius = size.height * 0.95f)
                    // A smaller bloom on the right, half a loop apart: the light never pulses as one block.
                    drawBloom(tokens, wave(t + 0.5f), centerX = size.width * 0.96f, radius = size.height * 0.62f, strength = 0.75f)
                },
        )
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

private const val MillisPerSecond = 1000

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
private fun grainBitmap(): ImageBitmap {
    val random = Random(GrainSize)
    val pixels = IntArray(GrainSize * GrainSize) {
        val value = random.nextInt(256)
        val alpha = random.nextInt(140)
        (alpha shl 24) or (value shl 16) or (value shl 8) or value
    }
    return Bitmap.createBitmap(pixels, GrainSize, GrainSize, Bitmap.Config.ARGB_8888).asImageBitmap()
}
