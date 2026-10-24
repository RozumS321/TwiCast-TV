package app.twicast.tv.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.TwiPrimaryButton
import app.twicast.tv.core.designsystem.component.TwiSecondaryButton
import app.twicast.tv.core.designsystem.icon.TwiIcons
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme

/**
 * Mockup "Главная · гость": guests browse through Twitch's site API, which may change, so the banner says what
 * signing in gives. «Войти» opens the sign-in; «Позже» hides it until the app starts again.
 */
@Composable
fun SignInBanner(onSignIn: () -> Unit, onLater: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwiTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.ButtonSpacing),
        modifier = modifier
            .padding(start = TwiDimens.ContentStartPadding, end = TwiDimens.ContentEndPadding)
            .fillMaxWidth()
            .background(colors.primaryContainer, TwiShapes.Card)
            .padding(horizontal = TwiDimens.SignInBannerHorizontalPadding, vertical = TwiDimens.SignInBannerVerticalPadding),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(TwiDimens.SignInBannerIconBox).background(colors.primary, TwiShapes.Chip),
        ) {
            Icon(TwiIcons.Profile, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(TwiDimens.NavigationIconSize))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TwiDimens.CardTextSpacing)) {
            Text(stringResource(R.string.home_sign_in_title), style = TwiTheme.typography.listTitle, color = colors.onSurface)
            Text(stringResource(R.string.home_sign_in_text), style = TwiTheme.typography.caption, color = colors.onSurfaceSecondary)
        }
        TwiPrimaryButton(text = stringResource(R.string.nav_sign_in), onClick = onSignIn)
        TwiSecondaryButton(text = stringResource(R.string.home_sign_in_later), onClick = onLater)
    }
}
