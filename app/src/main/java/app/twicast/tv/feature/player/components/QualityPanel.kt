package app.twicast.tv.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.requestInitialFocus
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import kotlinx.collections.immutable.ImmutableList

/** Right-side panel (mockup "Плеер · качество"). The list comes from the Twitch player itself. */
@Composable
fun QualityPanel(
    qualities: ImmutableList<VideoQuality>,
    currentQualityId: String?,
    onSelect: (VideoQuality) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.QualityRowSpacing),
        modifier = modifier
            .width(TwiDimens.QualityPanelWidth)
            .fillMaxHeight()
            .background(TwiTheme.colors.surface)
            .padding(horizontal = TwiDimens.QualityPanelHorizontalPadding, vertical = TwiDimens.QualityPanelVerticalPadding),
    ) {
        Text(stringResource(R.string.player_quality_title), style = TwiTheme.typography.rowTitle, color = TwiTheme.colors.onSurface)
        Text(
            text = stringResource(if (qualities.isEmpty()) R.string.player_quality_unavailable else R.string.player_quality_hint),
            style = TwiTheme.typography.caption,
            color = TwiTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(bottom = TwiDimens.SectionTitleSpacing),
        )
        // The current quality (or the first one) takes focus when the panel opens.
        val focusId = currentQualityId?.takeIf { id -> qualities.any { it.id == id } } ?: qualities.firstOrNull()?.id
        LazyColumn(verticalArrangement = Arrangement.spacedBy(TwiDimens.QualityRowSpacing)) {
            items(qualities, key = { it.id }) { quality ->
                QualityRow(
                    quality = quality,
                    selected = quality.id == currentQualityId,
                    onClick = { onSelect(quality) },
                    modifier = if (quality.id == focusId) Modifier.requestInitialFocus() else Modifier,
                )
            }
        }
    }
}

@Composable
private fun QualityRow(quality: VideoQuality, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = TwiTheme.colors
    val textColor = if (focused) colors.onPrimary else colors.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .height(TwiDimens.QualityRowHeight)
            .background(if (focused) colors.primary else Color.Transparent, TwiShapes.Button)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = TwiDimens.QualityRowPadding),
    ) {
        Text(
            text = if (quality.isAuto) stringResource(R.string.player_quality_auto) else quality.label,
            style = if (focused || selected) TwiTheme.typography.navigationLabelActive else TwiTheme.typography.navigationLabel,
            color = textColor,
        )
        if (selected) {
            Text(
                text = stringResource(R.string.player_quality_selected),
                style = TwiTheme.typography.tag,
                color = if (focused) colors.onPrimary else colors.onSurfaceVariant,
            )
        }
    }
}
