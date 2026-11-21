package app.twicast.tv.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.TwiSecondaryButton
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiTheme

/**
 * Mockup "Главная · гость": a quiet line above the hero, not a banner — guests browse fine, signing in only adds
 * their follows. «Войти» opens the sign-in.
 */
@Composable
fun SignInBanner(onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.ButtonSpacing),
        modifier = modifier,
    ) {
        Text(stringResource(R.string.home_sign_in_quiet), style = TwiTheme.typography.bodySmall, color = TwiTheme.colors.onSurfaceVariant)
        TwiSecondaryButton(text = stringResource(R.string.nav_sign_in), onClick = onSignIn)
    }
}
