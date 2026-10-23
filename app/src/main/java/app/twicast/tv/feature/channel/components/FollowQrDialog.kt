package app.twicast.tv.feature.channel.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.QrCode
import app.twicast.tv.core.designsystem.component.TwiSecondaryButton
import app.twicast.tv.core.designsystem.component.requestInitialFocus
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme

/** The channel on twitch.tv: the phone's Twitch app opens it from the QR. */
fun channelUrl(login: String): String = "https://www.twitch.tv/$login"

/**
 * Mockup "Канал · подписка через QR". Twitch lets no third-party app follow or unfollow (removed from its API in
 * 2021), so the viewer does it on the phone: a QR to the channel and three steps. A dialog keeps the remote's
 * focus inside; Back or "Готово" closes it.
 */
@Composable
fun FollowQrDialog(login: String, displayName: String, followed: Boolean, onDone: () -> Unit) {
    Dialog(onDismissRequest = onDone, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val colors = TwiTheme.colors
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TwiDimens.LoginPanelSpacing),
            modifier = Modifier
                .width(TwiDimens.FollowDialogWidth)
                .graphicsLayer {
                    shape = TwiShapes.Panel
                    shadowElevation = TwiDimens.LoginPanelShadowElevation.toPx()
                }
                .background(colors.surface, TwiShapes.Panel)
                .padding(horizontal = TwiDimens.LoginPanelHorizontalPadding, vertical = TwiDimens.LoginPanelVerticalPadding),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.LoginBlockSpacing), modifier = Modifier.weight(1f)) {
                Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.LoginHeaderSpacing)) {
                    Text(stringResource(R.string.follow_overline), style = TwiTheme.typography.overline, color = colors.primaryStrong)
                    Text(
                        stringResource(if (followed) R.string.follow_title_unfollow else R.string.follow_title_follow, displayName),
                        style = TwiTheme.typography.loginTitle,
                        color = colors.onSurface,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.LoginStepSpacing)) {
                    FollowStep(1, stringResource(R.string.follow_step_scan))
                    FollowStep(2, stringResource(if (followed) R.string.follow_step_unfollow else R.string.follow_step_follow))
                    FollowStep(3, stringResource(if (followed) R.string.follow_step_gone else R.string.follow_step_appears))
                }
                Text(stringResource(R.string.follow_why), style = TwiTheme.typography.caption, color = colors.onSurfaceVariant)
                TwiSecondaryButton(text = stringResource(R.string.follow_done), onClick = onDone, modifier = Modifier.requestInitialFocus())
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TwiDimens.QrCaptionSpacing),
            ) {
                Box(
                    modifier = Modifier
                        .size(TwiDimens.FollowQrSize)
                        .background(colors.surface, TwiShapes.Qr)
                        .border(TwiDimens.OutlineWidth, colors.outlineVariant, TwiShapes.Qr)
                        .padding(TwiDimens.QrPadding),
                ) {
                    QrCode(channelUrl(login), colors.onSurface, Modifier.fillMaxSize())
                }
                Text("twitch.tv/$login", style = TwiTheme.typography.caption, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FollowStep(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwiDimens.LoginStepSpacing)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(TwiDimens.LoginStepBadgeSize).background(TwiTheme.colors.primaryContainer, CircleShape),
        ) {
            Text(number.toString(), style = TwiTheme.typography.stepNumber, color = TwiTheme.colors.primaryStrong)
        }
        Text(text, style = TwiTheme.typography.step, color = TwiTheme.colors.onSurfaceSecondary)
    }
}
