package app.twicast.tv.feature.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.twicast.tv.core.designsystem.component.CategoryCardSkeleton
import app.twicast.tv.core.designsystem.component.StreamCardSkeleton
import app.twicast.tv.core.designsystem.component.rememberSkeletonPulse
import app.twicast.tv.core.designsystem.theme.TwiDimens

private const val SkeletonRows = 2

/** Loading placeholder for a grid screen: two rows of category covers or stream cards, one shared pulse. */
@Composable
fun GridSkeleton(columns: Int, cover: Boolean, modifier: Modifier = Modifier) {
    val pulse = rememberSkeletonPulse()
    Column(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.GridVerticalSpacing),
        modifier = modifier.padding(start = TwiDimens.ContentStartPadding, top = TwiDimens.GridTopPadding),
    ) {
        repeat(SkeletonRows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    if (cover) TwiDimens.CategoryGridHorizontalSpacing else TwiDimens.StreamGridHorizontalSpacing,
                ),
            ) {
                repeat(columns) {
                    if (cover) CategoryCardSkeleton(pulse) else StreamCardSkeleton(pulse, width = TwiDimens.StreamCardGridWidth)
                }
            }
        }
    }
}
