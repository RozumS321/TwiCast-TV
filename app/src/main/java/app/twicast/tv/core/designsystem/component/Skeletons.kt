package app.twicast.tv.core.designsystem.component

import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.runtime.remember
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.Dp
import app.twicast.tv.core.designsystem.theme.TwiAspect
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiMotion
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme

/** One sweep of the shine across the screen; the band is this share of the screen width. */
private const val SkeletonSweepMillis = 1600
private const val SkeletonBandFraction = 0.35f
private const val SkeletonShineAlpha = 0.07f
private const val SkeletonTitleFraction = 0.85f
private const val SkeletonCaptionFraction = 0.55f

/**
 * One shine shared by all skeletons of a screen (canvas: "Главная · загрузка"): a soft light band sweeping left to
 * right across the whole screen, so every block it passes lights up in turn, as one surface. [value] is 0..1.
 */
@Stable
class SkeletonPulse internal constructor(private val sweep: State<Float>) {
    val value: Float get() = sweep.value
}

@Composable
fun rememberSkeletonPulse(): SkeletonPulse {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val sweep = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SkeletonSweepMillis, easing = LinearEasing)),
        label = "sweep",
    )
    return SkeletonPulse(sweep)
}

@Composable
fun SkeletonBlock(pulse: SkeletonPulse, shape: Shape, modifier: Modifier = Modifier) {
    val color = TwiTheme.colors.surfaceVariant
    val shine = TwiTheme.colors.onSurface.copy(alpha = SkeletonShineAlpha)
    // Where this block sits on the screen, so the band crosses all blocks at the same screen position.
    val placement = remember { floatArrayOf(0f, 1f) }
    Box(
        modifier
            .onPlaced { coordinates ->
                placement[0] = coordinates.positionInRoot().x
                placement[1] = coordinates.findRootCoordinates().size.width.toFloat()
            }
            .drawWithCache {
                val outline = shape.createOutline(size, layoutDirection, this)
                onDrawBehind {
                    drawOutline(outline, color)
                    val screen = placement[1]
                    val band = screen * SkeletonBandFraction
                    val center = -band + pulse.value * (screen + 2 * band) - placement[0]
                    if (center + band > 0f && center - band < size.width) {
                        drawOutline(
                            outline,
                            Brush.horizontalGradient(
                                0f to Color.Transparent,
                                0.5f to shine,
                                1f to Color.Transparent,
                                startX = center - band,
                                endX = center + band,
                            ),
                        )
                    }
                }
            },
    )
}

/** Loading placeholder in the shape of [StreamCard]. */
@Composable
fun StreamCardSkeleton(pulse: SkeletonPulse, modifier: Modifier = Modifier, width: Dp = TwiDimens.StreamCardWidth) {
    Column(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.CardContentSpacing),
        modifier = modifier.width(width),
    ) {
        SkeletonBlock(pulse, TwiShapes.Thumbnail, Modifier.fillMaxWidth().aspectRatio(TwiAspect.Thumbnail))
        Row(
            horizontalArrangement = Arrangement.spacedBy(TwiDimens.CardContentSpacing),
            modifier = Modifier.padding(horizontal = TwiDimens.CardTextHorizontalPadding),
        ) {
            SkeletonBlock(pulse, CircleShape, Modifier.size(TwiDimens.AvatarSmall))
            SkeletonTextLines(pulse)
        }
    }
}

/** Loading placeholder in the shape of [CategoryCard]. */
@Composable
fun CategoryCardSkeleton(pulse: SkeletonPulse, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.CardContentSpacing),
        modifier = modifier.width(TwiDimens.CategoryCardWidth),
    ) {
        SkeletonBlock(pulse, TwiShapes.Cover, Modifier.fillMaxWidth().aspectRatio(TwiAspect.Cover))
        SkeletonTextLines(pulse, Modifier.padding(horizontal = TwiDimens.CardTextHorizontalPadding))
    }
}

@Composable
private fun SkeletonTextLines(pulse: SkeletonPulse, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.SkeletonLineSpacing), modifier = modifier) {
        SkeletonBlock(
            pulse,
            TwiShapes.SkeletonLine,
            Modifier.fillMaxWidth(SkeletonTitleFraction).height(TwiDimens.SkeletonLineHeight),
        )
        SkeletonBlock(
            pulse,
            TwiShapes.SkeletonLine,
            Modifier.fillMaxWidth(SkeletonCaptionFraction).height(TwiDimens.SkeletonCaptionHeight),
        )
    }
}
