package app.twicast.tv.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.EmptyState
import app.twicast.tv.core.designsystem.component.ErrorState
import app.twicast.tv.core.designsystem.component.KeepFocusedVisibleScroll
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.model.Stream
import app.twicast.tv.feature.common.appErrorMessage
import app.twicast.tv.feature.home.components.HomeSkeleton
import app.twicast.tv.feature.home.components.RecentRow
import app.twicast.tv.feature.home.components.StreamRow
import app.twicast.tv.data.repository.HistoryEntry
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

private object ContentType {
    const val HEADER = "header"
    const val ROW = "row"
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRetry: () -> Unit,
    onStreamClick: (Stream) -> Unit,
    modifier: Modifier = Modifier,
    onRecordingClick: (HistoryEntry) -> Unit = {},
    onChannelClick: (String) -> Unit = {},
) {
    when (state) {
        HomeUiState.Loading -> HomeSkeleton(modifier)
        is HomeUiState.Error -> ErrorState(appErrorMessage(state.error), onRetry, modifier)
        is HomeUiState.Empty -> Column(modifier.fillMaxSize()) {
            Greeting(state.userName, liveFollowedCount = 0, Modifier.padding(top = TwiDimens.ScreenTopPadding))
            EmptyState(stringResource(R.string.empty_streams))
        }
        is HomeUiState.Content -> KeepFocusedVisibleScroll {
            HomeContent(state, onStreamClick, onRecordingClick, onChannelClick, modifier)
        }
    }
}

/** Mockup "Главная · авторизован": greeting, then followed / recommended / regional rows. */
@Composable
private fun HomeContent(
    state: HomeUiState.Content,
    onStreamClick: (Stream) -> Unit,
    onRecordingClick: (HistoryEntry) -> Unit,
    onChannelClick: (String) -> Unit,
    modifier: Modifier,
) {
    val languageLabel = remember(state.languageCode) { languageName(state.languageCode) }
    // The first non-empty row takes the initial focus.
    val firstRowKey = when {
        state.followedLive.isNotEmpty() -> "followed"
        state.recent.isNotEmpty() -> "recent"
        state.recommended.isNotEmpty() -> "recommended"
        else -> "regional"
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = TwiDimens.ScreenTopPadding, bottom = TwiDimens.SectionSpacing),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "header", contentType = ContentType.HEADER) {
            Greeting(state.userName, state.followedLive.size, Modifier.padding(bottom = TwiDimens.HomeSectionSpacing))
        }
        val scrollToTop = Modifier.scrollToTopOnFocus(listState, scope)
        val topModifier = { key: String -> if (key == firstRowKey) scrollToTop else Modifier }
        row("followed", state.followedLive, topModifier) { key, streams ->
            StreamRow(
                title = stringResource(R.string.home_row_followed),
                hint = stringResource(R.string.home_row_followed_hint),
                streams = streams,
                onStreamClick = onStreamClick,
                followed = true,
                takesInitialFocus = key == firstRowKey,
            )
        }
        if (state.recent.isNotEmpty()) {
            item(key = "recent", contentType = ContentType.ROW) {
                Box(topModifier("recent")) {
                    RecentRow(
                        items = state.recent,
                        onStreamClick = onStreamClick,
                        onRecordingClick = onRecordingClick,
                        onChannelClick = onChannelClick,
                        takesInitialFocus = firstRowKey == "recent",
                    )
                }
            }
        }
        row("recommended", state.recommended, topModifier) { key, streams ->
            StreamRow(
                title = stringResource(R.string.home_row_recommended),
                hint = stringResource(R.string.home_row_recommended_hint),
                streams = streams,
                onStreamClick = onStreamClick,
                takesInitialFocus = key == firstRowKey,
            )
        }
        row("regional", state.regional, topModifier) { key, streams ->
            StreamRow(
                title = stringResource(R.string.home_row_regional),
                hint = languageLabel,
                streams = streams,
                onStreamClick = onStreamClick,
                takesInitialFocus = key == firstRowKey,
            )
        }
    }
}

/** Adds the row only when it has streams. */
private fun LazyListScope.row(
    key: String,
    streams: ImmutableList<Stream>,
    modifierFor: (key: String) -> Modifier,
    content: @Composable (key: String, streams: ImmutableList<Stream>) -> Unit,
) {
    if (streams.isNotEmpty()) {
        item(key = key, contentType = ContentType.ROW) {
            Box(modifierFor(key)) { content(key, streams) }
        }
    }
}

/**
 * Only cards take focus, so focus alone never scrolls back over the greeting above the first row:
 * coming back up to that row brings the whole list to the top.
 */
private fun Modifier.scrollToTopOnFocus(listState: LazyListState, scope: CoroutineScope): Modifier = onFocusChanged {
    if (it.hasFocus && listState.canScrollBackward) scope.launch { listState.animateScrollToItem(0) }
}

@Composable
private fun Greeting(userName: String, liveFollowedCount: Int, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.HomeHeaderSpacing),
        modifier = modifier.padding(horizontal = TwiDimens.ContentStartPadding),
    ) {
        Text(
            text = stringResource(R.string.home_greeting, userName),
            style = TwiTheme.typography.greeting,
            color = TwiTheme.colors.onSurface,
            modifier = Modifier.alignByBaseline(),
        )
        Text(
            text = if (liveFollowedCount > 0) {
                pluralStringResource(R.plurals.home_followed_live, liveFollowedCount, liveFollowedCount)
            } else {
                stringResource(R.string.home_followed_none)
            },
            style = TwiTheme.typography.body,
            color = TwiTheme.colors.onSurfaceVariant,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

/** "ru" → "Русский" in the device language. Twitch gives no region, so the hint names the language. */
private fun languageName(code: String): String {
    val deviceLocale = Locale.getDefault()
    return Locale.forLanguageTag(code).getDisplayLanguage(deviceLocale).replaceFirstChar { it.titlecase(deviceLocale) }
}
