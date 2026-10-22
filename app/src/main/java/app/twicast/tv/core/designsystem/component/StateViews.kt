package app.twicast.tv.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiTheme

/** Full-screen error with "Повторить", which takes focus so the remote can retry right away. */
@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val retryFocus = remember { FocusRequester() }
    MessageState(
        title = stringResource(R.string.error_title),
        message = message,
        modifier = modifier,
    ) {
        TwiPrimaryButton(
            text = stringResource(R.string.action_retry),
            onClick = onRetry,
            modifier = Modifier.focusRequester(retryFocus),
        )
    }
    LaunchedEffect(retryFocus) { retryFocus.requestFocus() }
}

/** Nothing to show (e.g. no live streams). */
@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    MessageState(title = null, message = message, modifier = modifier)
}

@Composable
private fun MessageState(
    title: String?,
    message: String,
    modifier: Modifier,
    action: @Composable () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwiDimens.SectionTitleSpacing, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = TwiDimens.ContentStartPadding),
    ) {
        if (title != null) {
            Text(title, style = TwiTheme.typography.rowTitle, color = TwiTheme.colors.onSurface)
        }
        Text(
            text = message,
            style = TwiTheme.typography.body,
            color = TwiTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = TwiDimens.MessageMaxWidth),
        )
        action()
    }
}

