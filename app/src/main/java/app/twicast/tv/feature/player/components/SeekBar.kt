package app.twicast.tv.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import app.twicast.tv.core.designsystem.format.formatPlaybackTime
import app.twicast.tv.core.designsystem.format.formatSeekDelta
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme

/** Track behind the played part: the overlay's text color, faint. */
private const val TrackAlpha = 0.22f

/**
 * A recording's progress (mockups "Плеер · запись, …"): time, bar with a thumb, length; over the thumb the
 * dialled jump ("+ 1 мин") while winding. [focusable] in the controls panel, where Left/Right wind ([onStep]
 * gets the direction and the key's auto-repeat count); on the bare video it only shows.
 */
@Composable
fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    deltaMs: Long?,
    focusable: Boolean,
    onStep: (forward: Boolean, repeat: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwiTheme.colors
    var focused by remember { mutableStateOf(false) }
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val positionText = formatPlaybackTime(positionMs)
    val durationText = formatPlaybackTime(durationMs)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.PlayerControlsSpacing),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(positionText, style = TwiTheme.typography.bodySmall, color = colors.onOverlay)
        BoxWithConstraints(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .weight(1f)
                .height(TwiDimens.SeekBarHeight)
                .semantics { contentDescription = "$positionText / $durationText" }
                .then(
                    if (focusable) {
                        Modifier
                            .onFocusChanged { focused = it.isFocused }
                            .onKeyEvent { event ->
                                val forward = when (event.key) {
                                    Key.DirectionRight -> true
                                    Key.DirectionLeft -> false
                                    else -> return@onKeyEvent false
                                }
                                if (event.type == KeyEventType.KeyDown) onStep(forward, event.nativeKeyEvent.repeatCount)
                                true
                            }
                            .focusable()
                    } else {
                        Modifier
                    },
                ),
        ) {
            val thumbX = maxWidth * fraction
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(TwiDimens.SeekTrackHeight)
                    .background(colors.onOverlay.copy(alpha = TrackAlpha), TwiShapes.Pill),
            )
            Box(
                Modifier
                    .width(thumbX)
                    .height(TwiDimens.SeekTrackHeight)
                    .background(colors.primary, TwiShapes.Pill),
            )
            // The ring marks where the remote winds: always on the bare-video bar, on focus in the panel.
            val ringed = !focusable || focused
            Box(
                Modifier
                    .offset(x = thumbX - TwiDimens.SeekThumbSize / 2)
                    .size(TwiDimens.SeekThumbSize)
                    .then(if (ringed) Modifier.border(TwiDimens.SeekThumbRing, colors.primary, CircleShape) else Modifier)
                    .padding(if (ringed) TwiDimens.SeekThumbRing else 0.dp)
                    .background(colors.onOverlay, CircleShape),
            )
            if (deltaMs != null) {
                Text(
                    text = formatSeekDelta(deltaMs),
                    style = TwiTheme.typography.bodySmall,
                    color = colors.onPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = thumbX - TwiDimens.SeekBubbleWidth / 2, y = -TwiDimens.SeekBubbleRise)
                        .width(TwiDimens.SeekBubbleWidth)
                        .background(colors.primary, TwiShapes.Card)
                        .padding(vertical = TwiDimens.SeekBubbleVerticalPadding),
                )
            }
        }
        Text(durationText, style = TwiTheme.typography.bodySmall, color = colors.onOverlayVariant)
    }
}
