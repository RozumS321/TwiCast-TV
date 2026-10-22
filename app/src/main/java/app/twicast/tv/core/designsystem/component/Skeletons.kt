package app.twicast.tv.core.designsystem.component

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

private const val SkeletonMinAlpha = 0.55f
private const val SkeletonMaxAlpha = 1f
private const val SkeletonTitleFraction = 0.85f
private const val SkeletonCaptionFraction = 0.55f

/** One pulse shared by all skeletons of a list, so a loading row runs a single animation. */
@Stable
class SkeletonPulse internal constructor(private val alpha: State<Float>) {
    val value: Float get() = alpha.value
}

@Composable
fun rememberSkeletonPulse(): SkeletonPulse {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha = transition.animateFloat(SkeletonMinAlpha, SkeletonMaxAlpha, TwiMotion.skeletonPulse(), label = "alpha")
    return SkeletonPulse(alpha)
}

@Composable
fun SkeletonBlock(pulse: SkeletonPulse, shape: Shape, modifier: Modifier = Modifier) {
    val color = TwiTheme.colors.surfaceVariant
    Box(
        modifier.drawBehind {
            drawOutline(shape.createOutline(size, layoutDirection, this), color, alpha = pulse.value)
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
