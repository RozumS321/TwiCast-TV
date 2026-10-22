package app.twicast.tv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.Text
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiScale
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme

/** Filter chip (40 high, pill). Selected = inverted surface. Focus: scale 1.05 + 3dp primary ring. */
@Composable
fun TwiChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwiTheme.colors
    val focusProgress = rememberFocusProgress()
    val interactionSource = remember { MutableInteractionSource() }
    val background = if (selected) colors.inverseSurface else colors.surface
    val borderColor = if (selected) Color.Transparent else colors.outline
    val textColor = if (selected) colors.inverseOnSurface else colors.onSurfaceSecondary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .focusTransform(focusProgress, TwiScale.ButtonFocused, TwiShapes.Chip)
            .background(background, TwiShapes.Chip)
            .border(TwiDimens.OutlineWidth, borderColor, TwiShapes.Chip)
            .focusRing(focusProgress, colors.primary, TwiShapes.Chip, TwiDimens.FocusBorderWidth)
            .trackFocus(focusProgress)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .height(TwiDimens.ChipHeight)
            .padding(horizontal = TwiDimens.ChipHorizontalPadding),
    ) {
        Text(text, style = TwiTheme.typography.chip, color = textColor, maxLines = 1)
    }
}
