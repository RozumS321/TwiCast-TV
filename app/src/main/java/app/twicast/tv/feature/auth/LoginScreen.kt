package app.twicast.tv.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.SkeletonBlock
import app.twicast.tv.core.designsystem.component.TwiSecondaryButton
import app.twicast.tv.core.designsystem.component.rememberSkeletonPulse
import app.twicast.tv.core.designsystem.component.requestInitialFocus
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.feature.auth.components.CodeCells
import app.twicast.tv.core.designsystem.component.QrCode
import app.twicast.tv.feature.common.appErrorMessage
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/** "XXXX-XXXX": cells shown while the code is loading. */
private const val PlaceholderCodeLength = 8
private const val CodeHalf = 4
private const val TickMillis = 1_000L
private const val SecondsInMinute = 60

/**
 * Sign-in panel (mockup "Вход · Device Flow"): steps and code on the left, QR on the right. On the first start
 * ([onContinueAsGuest] set) it also offers to go on without an account, saying what that costs.
 */
@Composable
fun LoginScreen(
    state: LoginUiState,
    onNewCode: () -> Unit,
    modifier: Modifier = Modifier,
    onContinueAsGuest: (() -> Unit)? = null,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TwiDimens.LoginPanelSpacing),
            modifier = Modifier
                .width(TwiDimens.LoginPanelWidth)
                .graphicsLayer {
                    shape = TwiShapes.Panel
                    shadowElevation = TwiDimens.LoginPanelShadowElevation.toPx()
                }
                .background(TwiTheme.colors.surface, TwiShapes.Panel)
                .padding(horizontal = TwiDimens.LoginPanelHorizontalPadding, vertical = TwiDimens.LoginPanelVerticalPadding),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(TwiDimens.LoginBlockSpacing),
                modifier = Modifier.weight(1f),
            ) {
                LoginHeader()
                LoginSteps()
                LoginCode(state)
                LoginStatus(state, onNewCode)
                if (onContinueAsGuest != null) GuestChoice(onContinueAsGuest)
            }
            LoginQr(state)
        }
    }
}

/** «Продолжить без входа» and what a guest goes without. */
@Composable
private fun GuestChoice(onContinueAsGuest: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.LoginStatusSpacing),
        modifier = Modifier
            .fillMaxWidth()
            .background(TwiTheme.colors.surfaceVariant, TwiShapes.Card)
            .padding(TwiDimens.LoginGuestPadding),
    ) {
        Text(
            stringResource(R.string.login_guest_warning),
            style = TwiTheme.typography.caption,
            color = TwiTheme.colors.onSurfaceSecondary,
            modifier = Modifier.weight(1f),
        )
        TwiSecondaryButton(text = stringResource(R.string.login_continue_as_guest), onClick = onContinueAsGuest)
    }
}

@Composable
private fun LoginHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.LoginHeaderSpacing)) {
        Text(
            text = stringResource(R.string.login_overline).uppercase(),
            style = TwiTheme.typography.overline,
            color = TwiTheme.colors.primaryStrong,
        )
        Text(stringResource(R.string.login_title), style = TwiTheme.typography.loginTitle, color = TwiTheme.colors.onSurface)
    }
}

@Composable
private fun LoginSteps() {
    val activateUrl = stringResource(R.string.login_activate_url)
    val firstStep = stringResource(R.string.login_step_1)
    val colors = TwiTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.LoginStepSpacing)) {
        LoginStep(
            number = 1,
            text = buildAnnotatedString {
                append(firstStep)
                append(' ')
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.onSurface)) { append(activateUrl) }
            },
        )
        LoginStep(number = 2, text = buildAnnotatedString { append(stringResource(R.string.login_step_2)) })
        LoginStep(number = 3, text = buildAnnotatedString { append(stringResource(R.string.login_step_3)) })
    }
}

@Composable
private fun LoginStep(number: Int, text: AnnotatedString) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.LoginStepSpacing),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(TwiDimens.LoginStepBadgeSize)
                .background(TwiTheme.colors.primaryContainer, CircleShape),
        ) {
            Text(number.toString(), style = TwiTheme.typography.stepNumber, color = TwiTheme.colors.primaryStrong)
        }
        Text(text, style = TwiTheme.typography.step, color = TwiTheme.colors.onSurfaceSecondary)
    }
}

@Composable
private fun LoginCode(state: LoginUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.LoginHeaderSpacing)) {
        Text(stringResource(R.string.login_your_code), style = TwiTheme.typography.label, color = TwiTheme.colors.onSurfaceVariant)
        val code = (state as? LoginUiState.WaitingForConfirmation)?.userCode?.let(::formatUserCode)
        CodeCells(code = code, cellCount = PlaceholderCodeLength)
    }
}

/** Status line: waiting + countdown, or what went wrong; "Новый код" is the only focusable element. */
@Composable
private fun LoginStatus(state: LoginUiState, onNewCode: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.LoginStatusSpacing),
        modifier = Modifier.fillMaxWidth(),
    ) {
        when (state) {
            LoginUiState.Loading -> StatusText(stringResource(R.string.login_requesting_code), highlighted = false)
            is LoginUiState.WaitingForConfirmation -> {
                WaitingIndicator()
                Countdown(state.expiresAtMillis)
            }
            LoginUiState.Expired -> StatusText(stringResource(R.string.login_code_expired), highlighted = false)
            is LoginUiState.Error -> StatusText(appErrorMessage(state.error), highlighted = false)
        }
        Spacer(Modifier.weight(1f))
        TwiSecondaryButton(
            text = stringResource(R.string.login_new_code),
            onClick = onNewCode,
            modifier = Modifier.requestInitialFocus(),
        )
    }
}

@Composable
private fun WaitingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.StatusDotSpacing),
    ) {
        Box(
            Modifier
                .border(TwiDimens.StatusDotRing, TwiTheme.colors.primaryContainer, CircleShape)
                .padding(TwiDimens.StatusDotRing)
                .size(TwiDimens.StatusDotSize)
                .background(TwiTheme.colors.primary, CircleShape),
        )
        StatusText(stringResource(R.string.login_waiting), highlighted = true)
    }
}

@Composable
private fun StatusText(text: String, highlighted: Boolean) {
    Text(
        text = text,
        style = if (highlighted) TwiTheme.typography.status else TwiTheme.typography.bodySmall,
        color = if (highlighted) TwiTheme.colors.primaryStrong else TwiTheme.colors.onSurfaceVariant,
    )
}

/** "Код действует ещё 9:41" — ticks once a second; only this text recomposes. */
@Composable
private fun Countdown(expiresAtMillis: Long) {
    val remainingSeconds by produceState(secondsUntil(expiresAtMillis), expiresAtMillis) {
        while (value > 0) {
            delay(TickMillis)
            value = secondsUntil(expiresAtMillis)
        }
    }
    val time = "%d:%02d".format(remainingSeconds / SecondsInMinute, remainingSeconds % SecondsInMinute)
    Text(
        text = stringResource(R.string.login_code_valid_for, time),
        style = TwiTheme.typography.bodySmall,
        color = TwiTheme.colors.onSurfaceVariant,
    )
}

@Composable
private fun LoginQr(state: LoginUiState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwiDimens.QrCaptionSpacing),
    ) {
        Box(
            modifier = Modifier
                .size(TwiDimens.QrSize)
                .graphicsLayer {
                    shape = TwiShapes.Qr
                    shadowElevation = TwiDimens.QrShadowElevation.toPx()
                }
                .background(TwiTheme.colors.surface, TwiShapes.Qr)
                .border(TwiDimens.OutlineWidth, TwiTheme.colors.outlineVariant, TwiShapes.Qr)
                .padding(TwiDimens.QrPadding),
        ) {
            if (state is LoginUiState.WaitingForConfirmation) {
                QrCode(state.verificationUri, TwiTheme.colors.onSurface, Modifier.fillMaxSize())
            } else {
                SkeletonBlock(rememberSkeletonPulse(), TwiShapes.CodeCell, Modifier.fillMaxSize())
            }
        }
        Text(stringResource(R.string.login_qr_caption), style = TwiTheme.typography.caption, color = TwiTheme.colors.onSurfaceVariant)
    }
}

/** Twitch sends "WDJBMJHT"; the mockup shows "WDJB-MJHT". */
private fun formatUserCode(code: String): String =
    if (code.length == PlaceholderCodeLength && !code.contains('-')) "${code.take(CodeHalf)}-${code.drop(CodeHalf)}" else code

private fun secondsUntil(epochMillis: Long): Long =
    TimeUnit.MILLISECONDS.toSeconds((epochMillis - System.currentTimeMillis()).coerceAtLeast(0))
