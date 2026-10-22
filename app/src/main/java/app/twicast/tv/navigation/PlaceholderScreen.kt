package app.twicast.tv.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import app.twicast.tv.core.designsystem.component.TwiSecondaryButton
import app.twicast.tv.core.designsystem.component.requestInitialFocus
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiTheme

/** Temporary stand-in for sections that arrive in later stages. */
@Composable
internal fun PlaceholderScreen(
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    @StringRes actionRes: Int,
    onAction: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.SectionTitleSpacing),
        modifier = Modifier
            .fillMaxSize()
            .padding(start = TwiDimens.ContentStartPadding, top = TwiDimens.ScreenTopPadding),
    ) {
        Text(stringResource(titleRes), style = TwiTheme.typography.screenTitle, color = TwiTheme.colors.onSurface)
        Text(stringResource(messageRes), style = TwiTheme.typography.body, color = TwiTheme.colors.onSurfaceVariant)
        // Opening a section moves focus into it, which also collapses the sidebar.
        TwiSecondaryButton(stringResource(actionRes), onClick = onAction, modifier = Modifier.requestInitialFocus())
    }
}
