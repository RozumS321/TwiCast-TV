package app.twicast.tv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.Text
import app.twicast.tv.core.designsystem.theme.TwiAspect
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiScale
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import coil3.compose.AsyncImage

/**
 * Category (game) card: 3:4 cover with optional rank "#N", name and viewers.
 * Focus: the whole card scales to 1.06; the 3dp ring and primary shadow sit on the cover only (no card background).
 * [width] is the grid column width (167 in the 6-column Categories grid).
 */
@Composable
fun CategoryCard(
    name: String,
    viewerCountText: String?,
    coverUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = TwiDimens.CategoryCardWidth,
    rank: Int? = null,
) {
    val focusProgress = rememberFocusProgress()
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.CardContentSpacing),
        modifier = modifier
            .width(width)
            .focusTransform(focusProgress, TwiScale.CardFocused, TwiShapes.Card)
            .trackFocus(focusProgress)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    ) {
        CategoryCover(coverUrl, rank, focusProgress)
        CategoryCardInfo(name, viewerCountText)
    }
}

@Composable
private fun CategoryCover(coverUrl: String?, rank: Int?, focusProgress: FocusProgress) {
    val colors = TwiTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(TwiAspect.Cover)
            // Ring sits outside the clipped layer below, so it is not cut off.
            .focusRing(focusProgress, colors.primary, TwiShapes.Cover, TwiDimens.FocusBorderWidth)
            .graphicsLayer {
                shape = TwiShapes.Cover
                clip = true
                shadowElevation = TwiDimens.FocusShadowElevation.toPx() * focusProgress.value
                ambientShadowColor = colors.primary
                spotShadowColor = colors.primary
            }
            .background(colors.surfaceVariant),
    ) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (rank != null) {
            RankBadge(rank, Modifier.align(Alignment.TopStart).padding(TwiDimens.RankInset))
        }
    }
}

@Composable
private fun CategoryCardInfo(name: String, viewerCountText: String?) {
    Column(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.CardTextSpacing),
        modifier = Modifier.padding(horizontal = TwiDimens.CategoryTextHorizontalPadding),
    ) {
        Text(
            text = name,
            style = TwiTheme.typography.cardTitle,
            color = TwiTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (viewerCountText != null) {
            Text(
                text = viewerCountText,
                style = TwiTheme.typography.caption,
                color = TwiTheme.colors.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
