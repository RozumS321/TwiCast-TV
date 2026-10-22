package app.twicast.tv.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import app.twicast.tv.feature.home.components.StreamRow
import kotlinx.collections.immutable.ImmutableList
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
) {
    when (state) {
        HomeUiState.Loading -> HomeSkeleton(modifier)
        is HomeUiState.Error -> ErrorState(appErrorMessage(state.error), onRetry, modifier)
        is HomeUiState.Empty -> Column(modifier.fillMaxSize()) {
            Greeting(state.userName, liveFollowedCount = 0, Modifier.padding(top = TwiDimens.ScreenTopPadding))
            EmptyState(stringResource(R.string.empty_streams))
        }
        is HomeUiState.Content -> KeepFocusedVisibleScroll { HomeContent(state, onStreamClick, modifier) }
    }
}

/** Mockup "Главная · авторизован": greeting, then followed / recommended / regional rows. */
@Composable
private fun HomeContent(state: HomeUiState.Content, onStreamClick: (Stream) -> Unit, modifier: Modifier) {
    val languageLabel = remember(state.languageCode) { languageName(state.languageCode) }
    // The first non-empty row takes the initial focus.
    val firstRowKey = when {
        state.followedLive.isNotEmpty() -> "followed"
        state.recommended.isNotEmpty() -> "recommended"
        else -> "regional"
    }
    LazyColumn(
        contentPadding = PaddingValues(top = TwiDimens.ScreenTopPadding, bottom = TwiDimens.SectionSpacing),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "header", contentType = ContentType.HEADER) {
            Greeting(state.userName, state.followedLive.size, Modifier.padding(bottom = TwiDimens.HomeSectionSpacing))
        }
        row("followed", state.followedLive) { key, streams ->
            StreamRow(
                title = stringResource(R.string.home_row_followed),
                hint = stringResource(R.string.home_row_followed_hint),
                streams = streams,
                onStreamClick = onStreamClick,
                followed = true,
                takesInitialFocus = key == firstRowKey,
            )
        }
        row("recommended", state.recommended) { key, streams ->
            StreamRow(
                title = stringResource(R.string.home_row_recommended),
                hint = stringResource(R.string.home_row_recommended_hint),
                streams = streams,
                onStreamClick = onStreamClick,
                takesInitialFocus = key == firstRowKey,
            )
        }
        row("regional", state.regional) { key, streams ->
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
    content: @Composable (key: String, streams: ImmutableList<Stream>) -> Unit,
) {
    if (streams.isNotEmpty()) item(key = key, contentType = ContentType.ROW) { content(key, streams) }
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
