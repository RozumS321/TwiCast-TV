package app.twicast.tv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme

/** Red "LIVE" badge for previews. [large] is the hero size. */
@Composable
fun LiveBadge(modifier: Modifier = Modifier, large: Boolean = false) {
    Text(
        text = stringResource(R.string.badge_live),
        style = if (large) TwiTheme.typography.badgeLarge else TwiTheme.typography.badge,
        color = TwiTheme.colors.onLive,
        modifier = modifier
            .background(TwiTheme.colors.live, if (large) TwiShapes.BadgeLarge else TwiShapes.Badge)
            .padding(
                horizontal = if (large) TwiDimens.BadgeLargeHorizontalPadding else TwiDimens.BadgeHorizontalPadding,
                vertical = if (large) TwiDimens.BadgeLargeVerticalPadding else TwiDimens.BadgeVerticalPadding,
            ),
    )
}

/** Viewer count on a dark translucent plate, e.g. "18,2 тыс". */
@Composable
fun ViewerCountBadge(text: String, modifier: Modifier = Modifier, large: Boolean = false) {
    Text(
        text = text,
        style = if (large) TwiTheme.typography.countLarge else TwiTheme.typography.count,
        color = TwiTheme.colors.onOverlay,
        maxLines = 1,
        modifier = modifier
            .background(TwiTheme.colors.overlay, if (large) TwiShapes.BadgeLarge else TwiShapes.Badge)
            .padding(
                horizontal = if (large) TwiDimens.CountLargeHorizontalPadding else TwiDimens.CountHorizontalPadding,
                vertical = if (large) TwiDimens.BadgeLargeVerticalPadding else TwiDimens.BadgeVerticalPadding,
            ),
    )
}

/** Category rank "#N" on a light plate over the cover. */
@Composable
fun RankBadge(rank: Int, modifier: Modifier = Modifier) {
    Text(
        text = "#$rank",
        style = TwiTheme.typography.rank,
        color = TwiTheme.colors.onSurface,
        modifier = modifier
            .background(TwiTheme.colors.surfaceOverlay, TwiShapes.Badge)
            .padding(horizontal = TwiDimens.BadgeHorizontalPadding, vertical = TwiDimens.RankVerticalPadding),
    )
}

/** Non-focusable label plate: language, tags, "3 ч в эфире". [dense] is the size in screen headers. */
@Composable
fun TwiTag(text: String, modifier: Modifier = Modifier, dense: Boolean = false) {
    Text(
        text = text,
        style = if (dense) TwiTheme.typography.tagDense else TwiTheme.typography.tag,
        color = TwiTheme.colors.onSurfaceSecondary,
        maxLines = 1,
        modifier = modifier
            .background(TwiTheme.colors.surfaceVariant, TwiShapes.Pill)
            .padding(
                horizontal = if (dense) TwiDimens.TagDenseHorizontalPadding else TwiDimens.TagHorizontalPadding,
                vertical = if (dense) TwiDimens.TagDenseVerticalPadding else TwiDimens.TagVerticalPadding,
            ),
    )
}
