package app.twicast.tv.feature.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.Avatar
import app.twicast.tv.core.designsystem.component.CategoryCard
import app.twicast.tv.core.designsystem.component.EmptyState
import app.twicast.tv.core.designsystem.component.ErrorState
import app.twicast.tv.core.designsystem.component.LiveBadge
import app.twicast.tv.core.designsystem.component.TwiTag
import app.twicast.tv.core.designsystem.component.focusRing
import app.twicast.tv.core.designsystem.component.focusTransform
import app.twicast.tv.core.designsystem.component.rememberFocusProgress
import app.twicast.tv.core.designsystem.component.trackFocus
import app.twicast.tv.core.designsystem.image.rememberTwitchImageUrl
import app.twicast.tv.core.designsystem.theme.TwiAspect
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiScale
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.model.Category
import app.twicast.tv.data.model.ChannelSearchResult
import app.twicast.tv.feature.common.appErrorMessage
import app.twicast.tv.feature.search.SearchResultsState
import kotlinx.coroutines.launch

private const val OfflineAvatarAlpha = 0.6f

/** Right column of the search screen: "Каналы" rows, then "Категории" covers. */
@Composable
fun SearchResults(
    state: SearchResultsState,
    onChannelClick: (ChannelSearchResult) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        SearchResultsState.Idle -> EmptyState(stringResource(R.string.search_hint), modifier)
        SearchResultsState.Empty -> EmptyState(stringResource(R.string.search_no_results), modifier)
        // Results replace each other while typing: keep the column quiet instead of flashing a skeleton.
        SearchResultsState.Loading -> Box(modifier.fillMaxSize())
        is SearchResultsState.Error -> ErrorState(appErrorMessage(state.error), onRetry, modifier)
        is SearchResultsState.Content -> LazyColumn(
            verticalArrangement = Arrangement.spacedBy(TwiDimens.ChannelRowSpacing),
            contentPadding = PaddingValues(top = TwiDimens.FocusBorderWidth, bottom = TwiDimens.ScreenTopPadding),
            modifier = modifier.fillMaxSize(),
        ) {
            if (state.channels.isNotEmpty()) {
                item(key = "channels-title") { SectionTitle(stringResource(R.string.search_channels)) }
                items(state.channels, key = { "channel-" + it.id }) { channel ->
                    ChannelRow(channel, onClick = { onChannelClick(channel) })
                }
            }
            if (state.categories.isNotEmpty()) {
                item(key = "categories") { CategoriesRow(state, onCategoryClick) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = TwiTheme.typography.rowTitleDense,
        color = TwiTheme.colors.onSurface,
        modifier = modifier.padding(bottom = TwiDimens.SectionTitleSpacing - TwiDimens.ChannelRowSpacing),
    )
}

@Composable
private fun ChannelRow(channel: ChannelSearchResult, onClick: () -> Unit) {
    val colors = TwiTheme.colors
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
        ChannelAvatar(channel)
        Column(Modifier.weight(1f)) {
            Text(channel.displayName, style = TwiTheme.typography.listTitle, color = colors.onSurface, maxLines = 1)
            Text(
                text = if (channel.isLive && channel.title.isNotEmpty()) "${channel.categoryName} · ${channel.title}" else channel.categoryName,
                style = TwiTheme.typography.caption,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (channel.isLive) LiveBadge(large = true) else TwiTag(stringResource(R.string.search_offline), dense = true)
    }
}

/** Live: red ring (as on the mockup); offline: faded. */
@Composable
private fun ChannelAvatar(channel: ChannelSearchResult) {
    if (channel.isLive) {
        Box(
            Modifier
                .border(TwiDimens.AvatarRingWidth, TwiTheme.colors.live, CircleShape)
                .padding(TwiDimens.AvatarRingWidth)
                .border(TwiDimens.AvatarRingWidth, TwiTheme.colors.surface, CircleShape)
                .padding(TwiDimens.AvatarRingWidth),
        ) {
            Avatar(channel.avatarUrl, TwiDimens.SearchAvatarSize - TwiDimens.AvatarRingWidth * 4)
        }
    } else {
        Avatar(channel.avatarUrl, TwiDimens.SearchAvatarSize, Modifier.alpha(OfflineAvatarAlpha))
    }
}

@Composable
private fun CategoriesRow(state: SearchResultsState.Content, onCategoryClick: (Category) -> Unit) {
    // Focusing a cover shows the whole block (title, covers, names): the card alone left its name under the edge.
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    Column(
        Modifier
            .bringIntoViewRequester(bringIntoView)
            .onFocusChanged { if (it.hasFocus) scope.launch { bringIntoView.bringIntoView() } }
            .padding(top = TwiDimens.SearchSectionSpacing - TwiDimens.ChannelRowSpacing),
    ) {
        SectionTitle(stringResource(R.string.search_categories))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(TwiDimens.SearchCategorySpacing),
            contentPadding = PaddingValues(
                start = TwiDimens.GridFocusInset,
                end = TwiDimens.GridFocusInset,
                top = TwiDimens.SectionTitleSpacing,
                bottom = TwiDimens.FocusOverflowPadding,
            ),
            modifier = Modifier.focusRestorer(),
        ) {
            items(state.categories, key = { it.id }) { category ->
                CategoryCard(
                    name = category.name,
                    viewerCountText = null,
                    coverUrl = rememberTwitchImageUrl(category.boxArtTemplate, TwiDimens.SearchCategoryWidth, TwiAspect.Cover),
                    onClick = { onCategoryClick(category) },
                    width = TwiDimens.SearchCategoryWidth,
                )
            }
        }
    }
}
