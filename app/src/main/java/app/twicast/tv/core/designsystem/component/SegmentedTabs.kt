package app.twicast.tv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.Text
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiScale
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import kotlinx.collections.immutable.ImmutableList

/** Two or three tabs in one pill (mockup "Подписки": "В эфире · N / Офлайн · N"). Selected = inverted surface. */
@Composable
fun SegmentedTabs(
    labels: ImmutableList<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwiTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.SegmentedPadding),
        modifier = modifier
            .background(colors.surface, TwiShapes.Segmented)
            .border(TwiDimens.OutlineWidth, colors.outlineVariant, TwiShapes.Segmented)
            .padding(TwiDimens.SegmentedPadding),
    ) {
        labels.forEachIndexed { index, label ->
            SegmentedTab(label, selected = index == selectedIndex, onClick = { onSelect(index) })
        }
    }
}

@Composable
private fun SegmentedTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = TwiTheme.colors
    val focusProgress = rememberFocusProgress()
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .focusTransform(focusProgress, TwiScale.ButtonFocused, TwiShapes.SegmentedTab)
            .background(if (selected) colors.inverseSurface else Color.Transparent, TwiShapes.SegmentedTab)
            .focusRing(focusProgress, colors.primary, TwiShapes.SegmentedTab, TwiDimens.FocusBorderWidth)
            .trackFocus(focusProgress)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .height(TwiDimens.SegmentedTabHeight)
            .padding(horizontal = TwiDimens.SegmentedTabPadding),
    ) {
        Text(label, style = TwiTheme.typography.tab, color = if (selected) colors.inverseOnSurface else colors.onSurfaceVariant)
    }
}
