package app.twicast.tv.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import app.twicast.tv.core.designsystem.theme.TwiMotion

/** One breath of the glow: in and out. */
private const val GlowBreathMillis = 4_000
private const val GlowMinAlpha = 0.35f
private const val GlowMaxAlpha = 0.9f
private const val GlowRadiusFactor = 0.62f
private val EmberCoral = Color(0xFFFF7A59)

/**
 * The "Уголёк" mark smoulders (canvas: "Логотип · варианты", D): a coral glow behind it slowly brightens and
 * fades. A radial gradient read in the draw phase only.
 */
@Composable
fun Modifier.emberGlow(): Modifier {
    val transition = rememberInfiniteTransition(label = "ember-glow")
    val alpha = transition.animateFloat(
        initialValue = GlowMinAlpha,
        targetValue = GlowMaxAlpha,
        animationSpec = infiniteRepeatable(tween(GlowBreathMillis / 2, easing = TwiMotion.Decelerate), RepeatMode.Reverse),
        label = "ember-glow-alpha",
    )
    return drawBehind {
        val radius = size.minDimension * GlowRadiusFactor
        drawCircle(
            brush = Brush.radialGradient(listOf(EmberCoral, Color.Transparent), center = center, radius = radius),
            radius = radius,
            alpha = alpha.value,
        )
    }
}
