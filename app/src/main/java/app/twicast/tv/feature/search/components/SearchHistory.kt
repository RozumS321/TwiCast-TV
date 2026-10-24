package app.twicast.tv.feature.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.Avatar
import app.twicast.tv.core.designsystem.component.LiveBadge
import app.twicast.tv.core.designsystem.component.TwiSecondaryButton
import app.twicast.tv.core.designsystem.component.focusRing
import app.twicast.tv.core.designsystem.component.focusTransform
import app.twicast.tv.core.designsystem.component.rememberFocusProgress
import app.twicast.tv.core.designsystem.component.trackFocus
import app.twicast.tv.core.designsystem.icon.TwiIcons
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiScale
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.feature.search.SearchHistoryItem
import kotlinx.collections.immutable.ImmutableList

private const val OfflineAvatarAlpha = 0.6f

/**
 * Mockup "Поиск · недавние": while nothing is typed, the queries and channels searched before, newest first, and
 * «Очистить историю». A query is typed again; a channel opens as from the results.
 */
@Composable
fun SearchHistory(
    items: ImmutableList<SearchHistoryItem>,
    onItemClick: (SearchHistoryItem) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.ChannelRowSpacing),
        contentPadding = PaddingValues(top = TwiDimens.FocusBorderWidth, bottom = TwiDimens.ScreenTopPadding),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "title") {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(TwiDimens.RowHintSpacing),
                modifier = Modifier.padding(bottom = TwiDimens.SectionTitleSpacing - TwiDimens.ChannelRowSpacing),
            ) {
                Text(
                    stringResource(R.string.search_recent),
                    style = TwiTheme.typography.rowTitleDense,
                    color = TwiTheme.colors.onSurface,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    stringResource(R.string.search_recent_hint),
                    style = TwiTheme.typography.caption,
                    color = TwiTheme.colors.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        items(items, key = { it.entry.key }) { item -> HistoryRow(item, onClick = { onItemClick(item) }) }
        item(key = "clear") {
            TwiSecondaryButton(
                text = stringResource(R.string.search_recent_clear),
                onClick = onClear,
                icon = TwiIcons.Erase,
                modifier = Modifier.padding(top = TwiDimens.ChannelRowSpacing),
            )
        }
    }
}

@Composable
private fun HistoryRow(item: SearchHistoryItem, onClick: () -> Unit) {
    val colors = TwiTheme.colors
    val entry = item.entry
    val focusProgress = rememberFocusProgress()
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.ChannelRowGap),
        modifier = Modifier
            .fillMaxWidth()
            .height(TwiDimens.ChannelRowHeight)
            .focusTransform(focusProgress, TwiScale.ButtonFocused, TwiShapes.Card)
            .background(colors.surface, TwiShapes.Card)
            .focusRing(focusProgress, colors.primary, TwiShapes.Card, TwiDimens.FocusBorderWidth)
            .trackFocus(focusProgress)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = TwiDimens.ChannelRowPadding),
    ) {
        when {
            !entry.isChannel -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(TwiDimens.SearchAvatarSize).background(colors.surfaceVariant, CircleShape),
            ) {
                Icon(TwiIcons.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(TwiDimens.NavigationIconSize))
            }
            item.live -> Box(
                Modifier
                    .border(TwiDimens.AvatarRingWidth, colors.live, CircleShape)
                    .padding(TwiDimens.AvatarRingWidth)
                    .border(TwiDimens.AvatarRingWidth, colors.surface, CircleShape)
                    .padding(TwiDimens.AvatarRingWidth),
            ) {
                Avatar(entry.avatarUrl, TwiDimens.SearchAvatarSize - TwiDimens.AvatarRingWidth * 4)
            }
            else -> Avatar(entry.avatarUrl, TwiDimens.SearchAvatarSize, Modifier.alpha(OfflineAvatarAlpha))
        }
        Column(Modifier.weight(1f)) {
            Text(entry.text, style = TwiTheme.typography.listTitle, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = if (entry.isChannel) {
                    listOf(stringResource(R.string.search_recent_channel), entry.categoryName).filter { it.isNotEmpty() }.joinToString(" · ")
                } else {
                    stringResource(R.string.search_recent_query)
                },
                style = TwiTheme.typography.caption,
                color = colors.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (item.live) LiveBadge(large = true)
    }
}
