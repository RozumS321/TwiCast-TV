package app.twicast.tv.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.TwiPrimaryButton
import app.twicast.tv.core.designsystem.component.TwiSecondaryButton
import app.twicast.tv.core.designsystem.component.glassOverVideo
import app.twicast.tv.core.designsystem.component.requestInitialFocus
import app.twicast.tv.core.designsystem.format.formatPlaybackTime
import app.twicast.tv.core.designsystem.icon.TwiIcons
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.sleep.SleepChoice

private val PanelWidth = 380.dp
private val AskWidth = 440.dp
private val WarningWidth = 520.dp

/** "29:41" for the island and the controls, or "до конца эфира"; null while the timer is off. */
@Composable
fun sleepLeftText(choice: SleepChoice, leftMillis: Long?): String? = when {
    choice == SleepChoice.Off -> null
    leftMillis != null -> formatPlaybackTime(leftMillis)
    else -> stringResource(R.string.sleep_until_end)
}

@Composable
private fun SleepChoice.label(): String = stringResource(
    when (this) {
        SleepChoice.Off -> R.string.sleep_off
        SleepChoice.Minute1 -> R.string.sleep_1_debug
        SleepChoice.Minutes15 -> R.string.sleep_15
        SleepChoice.Minutes30 -> R.string.sleep_30
        SleepChoice.Hour1 -> R.string.sleep_60
        SleepChoice.Hours2 -> R.string.sleep_120
        SleepChoice.StreamEnd -> R.string.sleep_stream_end
    },
)

/** Canvas "Плеер · таймер сна": the choices, the current one checked; a way to allow turning the TV off. */
@Composable
fun SleepTimerPanel(
    choice: SleepChoice,
    allowed: Boolean,
    onChoose: (SleepChoice) -> Unit,
    onAllow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwiTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.QualityRowSpacing),
        modifier = modifier
            .padding(TwiDimens.PlayerOverlayPadding)
            .width(PanelWidth)
            .glassOverVideo(TwiShapes.Panel)
            .padding(horizontal = TwiDimens.QualityPanelHorizontalPadding, vertical = TwiDimens.QualityPanelVerticalPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 6.dp)) {
            Icon(TwiIcons.Moon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.sleep_timer_title), style = TwiTheme.typography.rowTitle, color = colors.onSurface)
        }
        SleepChoice.entries.filter { it != SleepChoice.Minute1 || app.twicast.tv.BuildConfig.DEBUG }.forEach { option ->
            SleepRow(
                text = option.label(),
                selected = option == choice,
                onClick = { onChoose(option) },
                modifier = if (option == choice) Modifier.requestInitialFocus() else Modifier,
            )
        }
        if (!allowed) SleepRow(stringResource(R.string.sleep_allow_row), selected = false, onClick = onAllow, accent = true)
        Text(
            stringResource(if (allowed) R.string.sleep_hint_allowed else R.string.sleep_hint_not_allowed),
            style = TwiTheme.typography.caption,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, start = TwiDimens.QualityRowPadding, end = TwiDimens.QualityRowPadding),
        )
    }
}

@Composable
private fun SleepRow(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Boolean = false) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = TwiTheme.colors
    val textColor = when {
        focused -> colors.inverseOnSurface
        accent -> colors.primaryStrong
        else -> colors.onSurface
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .height(TwiDimens.QualityRowHeight)
            .background(if (focused) colors.inverseSurface else Color.Transparent, TwiShapes.Button)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = TwiDimens.QualityRowPadding),
    ) {
        Text(text, style = if (focused || selected) TwiTheme.typography.navigationLabelActive else TwiTheme.typography.navigationLabel, color = textColor)
        if (selected) Icon(TwiIcons.Check, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
    }
}

/** Canvas "Плеер · таймер сна: зачем разрешение" — once, when a time is first chosen without the right. */
@Composable
fun SleepAskCard(onAllow: () -> Unit, onOnlyStop: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwiTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .padding(TwiDimens.PlayerOverlayPadding)
            .width(AskWidth)
            .background(colors.dialog, TwiShapes.Panel)
            .padding(24.dp),
    ) {
        Box(Modifier.size(44.dp).background(colors.primary.copy(alpha = 0.18f), TwiShapes.Button), contentAlignment = Alignment.Center) {
            Icon(TwiIcons.Video, contentDescription = null, tint = colors.primaryStrong, modifier = Modifier.size(22.dp))
        }
        Text(stringResource(R.string.sleep_ask_title), style = TwiTheme.typography.rowTitle, color = colors.onSurface)
        Text(stringResource(R.string.sleep_ask_text), style = TwiTheme.typography.body, color = colors.onSurfaceVariant)
        Text(stringResource(R.string.sleep_ask_note), style = TwiTheme.typography.caption, color = colors.onSurfaceFaint)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
            TwiPrimaryButton(stringResource(R.string.sleep_ask_allow), onAllow, Modifier.fillMaxWidth().requestInitialFocus())
            TwiSecondaryButton(stringResource(R.string.sleep_ask_only_stop), onOnlyStop, Modifier.fillMaxWidth())
        }
    }
}

/** Canvas "Плеер · таймер сна: за минуту до конца": dims the video, counts down, offers more time or no timer. */
@Composable
fun SleepWarning(leftMillis: Long, onExtend: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwiTheme.colors
    Box(modifier.fillMaxSize().background(colors.scrim), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .width(WarningWidth)
                .background(colors.dialog, TwiShapes.Panel)
                .padding(28.dp),
        ) {
            Text(
                text = ((leftMillis + 999) / 1000).toString(),
                style = TwiTheme.typography.screenTitle,
                color = colors.primaryStrong,
            )
            Text(stringResource(R.string.sleep_warning_title), style = TwiTheme.typography.rowTitle, color = colors.onSurface, textAlign = TextAlign.Center)
            Text(stringResource(R.string.sleep_warning_text), style = TwiTheme.typography.body, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 6.dp)) {
                TwiPrimaryButton(stringResource(R.string.sleep_extend), onExtend, Modifier.requestInitialFocus())
                TwiSecondaryButton(stringResource(R.string.sleep_cancel), onCancel)
            }
        }
    }
}
