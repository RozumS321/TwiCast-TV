package app.twicast.tv.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.twicast.tv.core.designsystem.component.KeepFocusedVisibleScroll
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.data.model.Category
import app.twicast.tv.data.model.ChannelSearchResult
import app.twicast.tv.feature.search.components.QueryField
import app.twicast.tv.feature.search.components.SearchKeyboard
import app.twicast.tv.feature.search.components.SearchResults

/** Mockup "Поиск": query and keyboard on the left, results on the right. */
@Composable
fun SearchScreen(
    state: SearchUiState,
    voiceAvailable: Boolean,
    onKey: (Char) -> Unit,
    onSpace: () -> Unit,
    onErase: () -> Unit,
    onToggleLayout: () -> Unit,
    onVoice: () -> Unit,
    onChannelClick: (ChannelSearchResult) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.SearchColumnSpacing),
        modifier = modifier
            .fillMaxSize()
            .padding(start = TwiDimens.ContentStartPadding, end = TwiDimens.ContentEndPadding, top = TwiDimens.ScreenTopPadding),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(TwiDimens.SearchBlockSpacing),
            modifier = Modifier.width(TwiDimens.SearchPanelWidth),
        ) {
            QueryField(state.query)
            SearchKeyboard(
                layout = state.layout,
                voiceAvailable = voiceAvailable,
                onKey = onKey,
                onSpace = onSpace,
                onErase = onErase,
                onToggleLayout = onToggleLayout,
                onVoice = onVoice,
            )
        }
        Box(Modifier.weight(1f)) {
            KeepFocusedVisibleScroll {
                SearchResults(
                    state = state.results,
                    onChannelClick = onChannelClick,
                    onCategoryClick = onCategoryClick,
                    onRetry = onRetry,
                )
            }
        }
    }
}
