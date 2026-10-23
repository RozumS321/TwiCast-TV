package app.twicast.tv.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.requestInitialFocus
import app.twicast.tv.core.designsystem.icon.TwiIcons
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.feature.settings.SettingsSection

private class MenuEntry(val section: SettingsSection, val title: Int, val hint: Int, val icon: ImageVector)

private val MenuEntries = listOf(
    MenuEntry(SettingsSection.Player, R.string.settings_section_player, R.string.settings_section_player_hint, TwiIcons.Video),
    MenuEntry(SettingsSection.System, R.string.settings_section_system, R.string.settings_section_system_hint, TwiIcons.Settings),
)

private val MenuItemShape = RoundedCornerShape(TwiDimens.NavigationItemRadius)

/**
 * Sections of Settings, left of their content. A section opens as soon as its item takes focus.
 * Coming back from the content lands on the open section, like the main sidebar.
 */
@Composable
fun SettingsMenu(
    selected: SettingsSection,
    takesInitialFocus: Boolean,
    onSectionFocused: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedRequester = remember { FocusRequester() }
    Column(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.SettingsMenuItemSpacing),
        modifier = modifier
            .width(TwiDimens.SettingsMenuWidth)
            .focusProperties { onEnter = { selectedRequester.requestFocus() } }
            .focusGroup(),
    ) {
        MenuEntries.forEach { entry ->
            val isSelected = entry.section == selected
            MenuItem(
                entry = entry,
                isSelected = isSelected,
                onFocused = { onSectionFocused(entry.section) },
                modifier = when {
                    !isSelected -> Modifier
                    takesInitialFocus -> Modifier.focusRequester(selectedRequester).requestInitialFocus()
                    else -> Modifier.focusRequester(selectedRequester)
                },
            )
        }
    }
}

@Composable
private fun MenuItem(entry: MenuEntry, isSelected: Boolean, onFocused: () -> Unit, modifier: Modifier) {
    val colors = TwiTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val background = when {
        isFocused -> colors.primary
        isSelected -> colors.primaryContainer
        else -> Color.Transparent
    }
    val titleColor = when {
        isFocused -> colors.onPrimary
        isSelected -> colors.primaryStrong
        else -> colors.onSurface
    }
    val secondaryColor = if (isFocused) colors.onPrimary else colors.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.NavigationLabelGap),
        modifier = modifier
            .fillMaxWidth()
            .height(TwiDimens.SettingsMenuItemHeight)
            .onFocusChanged { if (it.isFocused) onFocused() }
            .background(background, MenuItemShape)
            // OK does nothing more: the section is already open. Clickable makes the item focusable.
            .clickable(interactionSource = interactionSource, indication = null) {}
            .padding(horizontal = TwiDimens.SettingsMenuItemPadding),
    ) {
        Icon(entry.icon, contentDescription = null, tint = secondaryColor, modifier = Modifier.size(TwiDimens.NavigationIconSize))
        Column {
            Text(
                stringResource(entry.title),
                style = if (isFocused || isSelected) TwiTheme.typography.navigationLabelActive else TwiTheme.typography.navigationLabel,
                color = titleColor,
            )
            Text(stringResource(entry.hint), style = TwiTheme.typography.caption, color = secondaryColor, maxLines = 1)
        }
    }
}
