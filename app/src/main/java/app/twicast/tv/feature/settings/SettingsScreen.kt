package app.twicast.tv.feature.settings

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import app.twicast.tv.BuildConfig
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.focusRing
import app.twicast.tv.core.designsystem.component.focusTransform
import app.twicast.tv.core.designsystem.component.rememberFocusProgress
import app.twicast.tv.core.designsystem.component.trackFocus
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiScale
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.model.AppLanguage
import app.twicast.tv.data.model.PlayerEngine
import app.twicast.tv.data.model.ThemeMode
import app.twicast.tv.feature.settings.components.AboutSection
import app.twicast.tv.feature.settings.components.ChatSection
import app.twicast.tv.feature.settings.components.NotificationsSection
import app.twicast.tv.feature.settings.components.LanguageSection
import app.twicast.tv.feature.settings.components.OptimizationSections
import app.twicast.tv.feature.settings.components.QualitySection
import app.twicast.tv.feature.settings.components.SettingsMenu
import app.twicast.tv.feature.settings.components.ThemeSection

private class PlayerOption(val engine: PlayerEngine, val title: Int, val tag: Int, val text: Int, val note: Int)

private val PlayerOptions = listOf(
    PlayerOption(
        PlayerEngine.TwiCast,
        R.string.settings_player_twicast,
        R.string.settings_player_twicast_tag,
        R.string.settings_player_twicast_text,
        R.string.settings_player_twicast_note,
    ),
    PlayerOption(
        PlayerEngine.Twitch,
        R.string.settings_player_twitch,
        R.string.settings_player_twitch_tag,
        R.string.settings_player_twitch_text,
        R.string.settings_player_twitch_note,
    ),
)

/**
 * Mockups "Настройки · Плеер" / "Настройки · Система": the section menu on the left, its content on the right.
 * Entering the screen focuses the open section in the menu; after a language change, the language chips.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onSectionFocused: (SettingsSection) -> Unit,
    onPlayerSelected: (PlayerEngine) -> Unit,
    onDefaultQualitySelected: (DefaultQuality) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onOpenChatChanged: (Boolean) -> Unit,
    onNotifyLiveStartsChanged: (Boolean) -> Unit,
    onWarmTwitchPlayerChanged: (Boolean) -> Unit,
    onFreeMemoryInBackgroundChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.SettingsContentSpacing),
        modifier = modifier
            .fillMaxSize()
            .padding(start = TwiDimens.ContentStartPadding, end = TwiDimens.ContentEndPadding, top = TwiDimens.ScreenTopPadding),
    ) {
        Text(stringResource(R.string.settings_title), style = TwiTheme.typography.screenTitle, color = TwiTheme.colors.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(TwiDimens.SettingsSectionsSpacing)) {
            SettingsMenu(state.section, takesInitialFocus = !state.focusLanguage, onSectionFocused)
            // Scrolls when a section is taller than the screen ("Плеер" with notifications); focus brings cards into view.
            Column(
                verticalArrangement = Arrangement.spacedBy(TwiDimens.SettingsContentSpacing),
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = TwiDimens.ScreenTopPadding),
            ) {
                when (state.section) {
                    SettingsSection.Player -> {
                        PlayerSection(state.playerEngine, onPlayerSelected)
                        QualitySection(state.defaultQuality, state.loaded, onDefaultQualitySelected)
                        ChatSection(state.openChatWithStream, onOpenChatChanged)
                    }
                    SettingsSection.System -> {
                        ThemeSection(state.themeMode, onThemeSelected)
                        LanguageSection(state.language, state.focusLanguage, onLanguageSelected)
                        NotificationsSection(state.notifyLiveStarts, onNotifyLiveStartsChanged)
                        AboutSection(BuildConfig.VERSION_NAME)
                    }
                    SettingsSection.Optimization -> OptimizationSections(
                        warmPlayer = state.warmTwitchPlayer,
                        warmAvailable = state.playerEngine == PlayerEngine.Twitch,
                        lowMemory = state.lowMemory,
                        freeMemory = state.freeMemoryInBackground,
                        onWarmPlayerChanged = onWarmTwitchPlayerChanged,
                        onFreeMemoryChanged = onFreeMemoryInBackgroundChanged,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerSection(selected: PlayerEngine?, onPlayerSelected: (PlayerEngine) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.HomeRowTitleSpacing)) {
        Text(stringResource(R.string.settings_player), style = TwiTheme.typography.rowTitleDense, color = TwiTheme.colors.onSurface)
        if (selected != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(TwiDimens.ChannelGridSpacing), modifier = Modifier.fillMaxWidth()) {
                PlayerOptions.forEach { option ->
                    PlayerCard(
                        option = option,
                        selected = option.engine == selected,
                        onClick = { onPlayerSelected(option.engine) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Text(
            stringResource(R.string.settings_player_footer),
            style = TwiTheme.typography.caption,
            color = TwiTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlayerCard(option: PlayerOption, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = TwiTheme.colors
    val focusProgress = rememberFocusProgress()
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.ChannelAboutSpacing),
        modifier = modifier
            .focusTransform(
                progress = focusProgress,
                focusedScale = TwiScale.CardFocused,
                shape = TwiShapes.Banner,
                shadowElevation = TwiDimens.FocusShadowElevation,
                shadowColor = colors.primary,
            )
            .background(colors.surface, TwiShapes.Banner)
            .border(TwiDimens.OutlineWidth, if (selected) colors.primary else colors.outlineVariant, TwiShapes.Banner)
            .focusRing(focusProgress, colors.primary, TwiShapes.Banner, TwiDimens.FocusBorderWidth)
            .trackFocus(focusProgress)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(TwiDimens.ChannelAboutPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(option.title), style = TwiTheme.typography.listTitle, color = colors.onSurface, modifier = Modifier.weight(1f))
            RadioMark(selected)
        }
        val recommended = option.engine == PlayerEngine.TwiCast
        Text(
            stringResource(option.tag),
            style = TwiTheme.typography.tagDense,
            color = if (recommended) colors.primaryStrong else colors.onSurfaceVariant,
            modifier = Modifier
                .background(if (recommended) colors.primaryContainer else colors.background, TwiShapes.Pill)
                .padding(horizontal = TwiDimens.BadgePillHorizontalPadding, vertical = TwiDimens.BadgePillVerticalPadding),
        )
        Text(stringResource(option.text), style = TwiTheme.typography.body, color = colors.onSurfaceSecondary)
        Text(stringResource(option.note), style = TwiTheme.typography.caption, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun RadioMark(selected: Boolean) {
    val colors = TwiTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(TwiDimens.SettingsRadioSize)
            .border(TwiDimens.SettingsRadioBorder, if (selected) colors.primary else colors.onSurfaceFaint, CircleShape),
    ) {
        if (selected) Box(Modifier.size(TwiDimens.SettingsRadioDot).background(colors.primary, CircleShape))
    }
}
