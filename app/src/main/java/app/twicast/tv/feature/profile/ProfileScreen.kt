package app.twicast.tv.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.focusRing
import app.twicast.tv.core.designsystem.component.focusTransform
import app.twicast.tv.core.designsystem.component.rememberFocusProgress
import app.twicast.tv.core.designsystem.component.requestInitialFocus
import app.twicast.tv.core.designsystem.component.trackFocus
import app.twicast.tv.core.designsystem.icon.TwiIcons
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiScale
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import coil3.compose.AsyncImage

private const val InitialsLength = 2
private const val NoValue = "—"

/** Mockup "Профиль": avatar, name, two counters, sign-out; app version below the panel. */
@Composable
fun ProfileScreen(state: ProfileUiState, versionName: String, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwiDimens.ProfileFooterSpacing, Alignment.CenterVertically),
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TwiDimens.ProfileSpacing),
            modifier = Modifier
                .width(TwiDimens.ProfilePanelWidth)
                .graphicsLayer {
                    shape = TwiShapes.Panel
                    shadowElevation = TwiDimens.LoginPanelShadowElevation.toPx()
                }
                .background(TwiTheme.colors.surface, TwiShapes.Panel)
                .padding(
                    start = TwiDimens.UpdatePanelHorizontalPadding,
                    end = TwiDimens.UpdatePanelHorizontalPadding,
                    top = TwiDimens.ProfilePanelTopPadding,
                    bottom = TwiDimens.ProfilePanelBottomPadding,
                ),
        ) {
            ProfileAvatar(state)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.user?.displayName.orEmpty(), style = TwiTheme.typography.greeting, color = TwiTheme.colors.onSurface)
                Text(stringResource(R.string.profile_signed_in_with), style = TwiTheme.typography.body, color = TwiTheme.colors.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(TwiDimens.ButtonSpacing), modifier = Modifier.fillMaxWidth()) {
                Stat(state.followsCount, stringResource(R.string.profile_follows), Modifier.weight(1f))
                Stat(state.liveCount, stringResource(R.string.profile_live_now), Modifier.weight(1f))
            }
            SignOutButton(onSignOut)
        }
        Text(
            text = stringResource(R.string.profile_version, versionName),
            style = TwiTheme.typography.caption,
            color = TwiTheme.colors.onSurfaceVariant,
        )
    }
}

/** 128 avatar with a soft primary halo; initials until the picture loads (or if there is none). */
@Composable
private fun ProfileAvatar(state: ProfileUiState) {
    val colors = TwiTheme.colors
    val user = state.user
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(colors.primaryContainer, CircleShape)
            .padding(TwiDimens.ProfileAvatarHalo)
            .size(TwiDimens.ProfileAvatarSize)
            .clip(CircleShape)
            .background(colors.primary),
    ) {
        Text(
            text = user?.displayName.orEmpty().take(InitialsLength).uppercase(),
            style = TwiTheme.typography.avatarInitials,
            color = colors.onPrimary,
        )
        if (user?.avatarUrl != null) {
            AsyncImage(model = user.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun Stat(value: Int?, label: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(TwiTheme.colors.background, TwiShapes.Button)
            .padding(horizontal = TwiDimens.ProfileStatSidePadding, vertical = TwiDimens.ProfileStatPadding),
    ) {
        Text(value?.toString() ?: NoValue, style = TwiTheme.typography.statValue, color = TwiTheme.colors.onSurface)
        Text(label, style = TwiTheme.typography.caption, color = TwiTheme.colors.onSurfaceVariant)
    }
}

/** Full-width dark button (mockup); the only focusable element, so it takes focus. */
@Composable
private fun SignOutButton(onSignOut: () -> Unit) {
    val colors = TwiTheme.colors
    val focusProgress = rememberFocusProgress()
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.ButtonIconGap, Alignment.CenterHorizontally),
        modifier = Modifier
            .padding(top = TwiDimens.KeySpacing)
            .fillMaxWidth()
            .height(TwiDimens.ProfileButtonHeight)
            .requestInitialFocus()
            .focusTransform(focusProgress, TwiScale.ButtonFocused, TwiShapes.Button)
            .background(colors.inverseSurface, TwiShapes.Button)
            .focusRing(focusProgress, colors.primary, TwiShapes.Button, TwiDimens.FocusBorderWidth)
            .trackFocus(focusProgress)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onSignOut),
    ) {
        Icon(TwiIcons.SignOut, contentDescription = null, tint = colors.inverseOnSurface, modifier = Modifier.size(TwiDimens.ButtonIconSize))
        Text(stringResource(R.string.action_sign_out), style = TwiTheme.typography.button, color = colors.inverseOnSurface)
    }
}
