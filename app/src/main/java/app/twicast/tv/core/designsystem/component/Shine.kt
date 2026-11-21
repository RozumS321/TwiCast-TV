package app.twicast.tv.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

/** One sweep and the pause after it. */
private const val ShineLoopMillis = 6_000
/** The part of the loop the light takes to cross the mark. */
private const val ShineSweepFraction = 0.22f
private const val ShineAlpha = 0.55f
private const val ShineBandWidth = 0.35f

/**
 * Logo "Shine": every few seconds a soft band of light slides across the mark (snapblocks' idea, drawn here with a
 * gradient). Only over the mark's own pixels; animated in the draw phase.
 */
@Composable
fun Modifier.shine(): Modifier {
    val transition = rememberInfiniteTransition(label = "shine")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(ShineLoopMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "shine-phase",
    )
    return this
        // Offscreen so the light is masked to the mark (SrcAtop) instead of painting a rectangle.
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val t = phase.value / ShineSweepFraction
            if (t > 1f) return@drawWithContent
            val travel = size.width * (1f + ShineBandWidth * 2)
            val x = -size.width * ShineBandWidth + travel * t
            drawRect(
                brush = Brush.linearGradient(
                    0f to Color.Transparent,
                    0.5f to Color.White.copy(alpha = ShineAlpha),
                    1f to Color.Transparent,
                    start = Offset(x - size.width * ShineBandWidth, 0f),
                    end = Offset(x, size.height),
                ),
                blendMode = BlendMode.SrcAtop,
            )
        }
}
