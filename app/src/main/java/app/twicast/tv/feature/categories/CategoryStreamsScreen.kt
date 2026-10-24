package app.twicast.tv.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.EmptyState
import app.twicast.tv.core.designsystem.component.ErrorState
import app.twicast.tv.core.designsystem.component.KeepFocusedVisibleScroll
import app.twicast.tv.core.designsystem.component.LoadMoreWhenNearEnd
import app.twicast.tv.core.designsystem.component.StreamCard
import app.twicast.tv.core.designsystem.component.TwiChip
import app.twicast.tv.core.designsystem.component.TwiIconButton
import app.twicast.tv.core.designsystem.component.requestInitialFocus
import app.twicast.tv.core.designsystem.format.formatUptimeShort
import app.twicast.tv.core.designsystem.format.formatViewerCount
import app.twicast.tv.core.designsystem.icon.TwiIcons
import app.twicast.tv.core.designsystem.image.rememberTwitchImageUrl
import app.twicast.tv.core.designsystem.theme.TwiAspect
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.model.Stream
import app.twicast.tv.feature.common.GridSkeleton
import app.twicast.tv.feature.common.appErrorMessage
import coil3.compose.AsyncImage
import java.util.Locale

private const val Columns = 4
private const val LoadMoreThreshold = Columns * 2

/** Mockup "Стримы категории": back, cover, name, language chips, 4-column grid of streams. */
@Composable
fun CategoryStreamsScreen(
    state: CategoryStreamsUiState,
    onBack: () -> Unit,
    onLanguageSelected: (String?) -> Unit,
    onStreamClick: (Stream) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(top = TwiDimens.ScreenTopPadding)) {
        CategoryHeader(state, onBack, onLanguageSelected)
        when (val grid = state.grid) {
            StreamsGridState.Loading -> GridSkeleton(columns = Columns, cover = false)
            StreamsGridState.Empty -> EmptyState(stringResource(R.string.category_streams_empty))
            is StreamsGridState.Error -> ErrorState(appErrorMessage(grid.error), onRetry)
            is StreamsGridState.Content -> KeepFocusedVisibleScroll { StreamsGrid(grid, onStreamClick, onLoadMore) }
        }
    }
}

@Composable
private fun CategoryHeader(state: CategoryStreamsUiState, onBack: () -> Unit, onLanguageSelected: (String?) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.CategoryHeaderSpacing),
        modifier = Modifier.fillMaxWidth().padding(horizontal = TwiDimens.ContentStartPadding),
    ) {
        TwiIconButton(TwiIcons.Back, stringResource(R.string.category_streams_back), onClick = onBack)
        Box(
            Modifier
                .size(TwiDimens.CategoryHeaderCoverWidth, TwiDimens.CategoryHeaderCoverHeight)
                .clip(TwiShapes.Cover)
                .background(TwiTheme.colors.surfaceVariant),
        ) {
            AsyncImage(
                model = rememberTwitchImageUrl(state.category.boxArtTemplate, TwiDimens.CategoryHeaderCoverWidth, TwiAspect.Cover),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = state.category.categoryName,
            style = TwiTheme.typography.screenTitle,
            color = TwiTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(TwiDimens.ChipSpacing)) {
            CategoryLanguages.forEach { language ->
                TwiChip(
                    text = language?.let(::languageName) ?: stringResource(R.string.category_streams_all_languages),
                    selected = language == state.language,
                    onClick = { onLanguageSelected(language) },
                )
            }
        }
    }
}

@Composable
private fun StreamsGrid(grid: StreamsGridState.Content, onStreamClick: (Stream) -> Unit, onLoadMore: () -> Unit) {
    val gridState = rememberLazyGridState()
    gridState.LoadMoreWhenNearEnd(LoadMoreThreshold, onLoadMore)
    LazyVerticalGrid(
        columns = GridCells.Fixed(Columns),
        state = gridState,
        contentPadding = PaddingValues(
            start = TwiDimens.GridFocusInset,
            end = TwiDimens.GridFocusInset,
            top = TwiDimens.GridTopPadding,
            bottom = TwiDimens.FocusOverflowPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.StreamGridHorizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(TwiDimens.GridVerticalSpacing),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = TwiDimens.ContentStartPadding - TwiDimens.GridFocusInset)
            .focusRestorer(),
    ) {
        itemsIndexed(grid.streams, key = { _, stream -> stream.id }, contentType = { _, _ -> "stream" }) { index, stream ->
            StreamCard(
                title = stream.title,
                subtitle = stream.channelName,
                viewerCountText = formatViewerCount(stream.viewerCount),
                thumbnailUrl = rememberTwitchImageUrl(stream.thumbnailTemplate, TwiDimens.StreamCardGridWidth, TwiAspect.Thumbnail),
                avatarUrl = stream.avatarUrl,
                onClick = { onStreamClick(stream) },
                width = TwiDimens.StreamCardGridWidth,
                uptimeText = stream.startedAtMillis?.let { formatUptimeShort(it, System.currentTimeMillis()) },
                modifier = if (index == 0) Modifier.requestInitialFocus() else Modifier,
            )
        }
    }
}

/** "uk" → "Українська": each language in its own name, as on the mockup chips. */
private fun languageName(code: String): String {
    val locale = Locale.forLanguageTag(code)
    return locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
}
