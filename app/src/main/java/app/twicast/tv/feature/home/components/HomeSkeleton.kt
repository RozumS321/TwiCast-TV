package app.twicast.tv.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.twicast.tv.core.designsystem.component.SkeletonBlock
import app.twicast.tv.core.designsystem.component.SkeletonPulse
import app.twicast.tv.core.designsystem.component.StreamCardSkeleton
import app.twicast.tv.core.designsystem.component.rememberSkeletonPulse
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiShapes

private const val SkeletonRows = 3
private const val SkeletonCards = 6

/** Loading layout in the shape of home: greeting + rows of dense cards. One shared pulse. */
@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    val pulse = rememberSkeletonPulse()
    Column(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.HomeSectionSpacing),
        modifier = modifier
            .fillMaxSize()
            .padding(start = TwiDimens.ContentStartPadding, top = TwiDimens.ScreenTopPadding),
    ) {
        SkeletonBlock(pulse, TwiShapes.SkeletonLine, Modifier.size(TwiDimens.SkeletonGreetingWidth, TwiDimens.SkeletonTitleHeight))
        repeat(SkeletonRows) { RowSkeleton(pulse) }
    }
}

@Composable
private fun RowSkeleton(pulse: SkeletonPulse) {
    Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.HomeRowTitleSpacing)) {
        SkeletonBlock(pulse, TwiShapes.SkeletonLine, Modifier.size(TwiDimens.SkeletonRowTitleWidth, TwiDimens.SkeletonLineHeight))
        Row(horizontalArrangement = Arrangement.spacedBy(TwiDimens.HomeCardSpacing)) {
            repeat(SkeletonCards) { StreamCardSkeleton(pulse, width = TwiDimens.StreamCardDenseWidth) }
        }
    }
}
