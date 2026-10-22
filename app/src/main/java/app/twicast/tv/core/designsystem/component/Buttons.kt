package app.twicast.tv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiScale
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme

/**
 * Main action ("Смотреть"): primary with its shadow (0 10 24 primary@32%) — one per screen, so the resting shadow is cheap.
 * Focus: scale 1.05, stronger shadow, 3dp primaryStrong ring.
 */
@Composable
fun TwiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = TwiTheme.colors
    val focusProgress = rememberFocusProgress()
    ButtonContainer(
        onClick = onClick,
        focusProgress = focusProgress,
        horizontalPadding = TwiDimens.ButtonHorizontalPadding,
        modifier = modifier
            .focusTransform(
                progress = focusProgress,
                focusedScale = TwiScale.ButtonFocused,
                shape = TwiShapes.Button,
                shadowElevation = TwiDimens.ButtonShadowElevation,
                shadowColor = colors.primary,
                restShadowElevation = TwiDimens.ButtonRestShadowElevation,
            )
            .background(colors.primary, TwiShapes.Button)
            .focusRing(focusProgress, colors.primaryStrong, TwiShapes.Button, TwiDimens.FocusBorderWidth),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(TwiDimens.ButtonIconSize))
        }
        Text(text, style = TwiTheme.typography.button, color = colors.onPrimary)
    }
}

/** Secondary action ("Канал"): surface with outline. Focus: scale 1.05 + 3dp primary ring. */
@Composable
fun TwiSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = TwiTheme.colors
    val focusProgress = rememberFocusProgress()
    ButtonContainer(
        onClick = onClick,
        focusProgress = focusProgress,
        horizontalPadding = TwiDimens.SecondaryButtonHorizontalPadding,
        modifier = modifier.outlinedSurface(focusProgress),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(TwiDimens.ButtonIconSize))
        }
        Text(text, style = TwiTheme.typography.buttonSecondary, color = colors.onSurface)
    }
}

/** Square icon-only button, e.g. "Назад" in a screen header (48, outline). Same focus as [TwiSecondaryButton]. */
@Composable
fun TwiIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusProgress = rememberFocusProgress()
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .outlinedSurface(focusProgress)
            .trackFocus(focusProgress)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .size(TwiDimens.IconButtonSize),
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = TwiTheme.colors.onSurface,
            modifier = Modifier.size(TwiDimens.IconButtonIconSize),
        )
    }
}

@Composable
private fun Modifier.outlinedSurface(focusProgress: FocusProgress): Modifier {
    val colors = TwiTheme.colors
    return focusTransform(focusProgress, TwiScale.ButtonFocused, TwiShapes.Button)
        .background(colors.surface, TwiShapes.Button)
        .border(TwiDimens.OutlineWidth, colors.outline, TwiShapes.Button)
        .focusRing(focusProgress, colors.primary, TwiShapes.Button, TwiDimens.FocusBorderWidth)
}

@Composable
private fun ButtonContainer(
    onClick: () -> Unit,
    focusProgress: FocusProgress,
    horizontalPadding: Dp,
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.ButtonIconGap),
        content = content,
        modifier = modifier
            .trackFocus(focusProgress)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .height(TwiDimens.ButtonHeight)
            .padding(horizontal = horizontalPadding),
    )
}
