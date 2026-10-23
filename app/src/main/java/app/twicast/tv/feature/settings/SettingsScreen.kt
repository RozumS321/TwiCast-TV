package app.twicast.tv.feature.settings

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
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.TwiChip
import app.twicast.tv.core.designsystem.component.focusRing
import app.twicast.tv.core.designsystem.component.focusTransform
import app.twicast.tv.core.designsystem.component.rememberFocusProgress
import app.twicast.tv.core.designsystem.component.requestInitialFocus
import app.twicast.tv.core.designsystem.component.trackFocus
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiScale
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.model.AppLanguage
import app.twicast.tv.data.model.PlayerEngine
import app.twicast.tv.data.model.ThemeMode

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

private class LanguageOption(val language: AppLanguage, val title: Int)

private val LanguageOptions = listOf(
    LanguageOption(AppLanguage.Russian, R.string.settings_language_russian),
    LanguageOption(AppLanguage.Ukrainian, R.string.settings_language_ukrainian),
    LanguageOption(AppLanguage.English, R.string.settings_language_english),
)

private class ThemeOption(val mode: ThemeMode, val title: Int)

private val ThemeOptions = listOf(
    ThemeOption(ThemeMode.System, R.string.settings_theme_system),
    ThemeOption(ThemeMode.Light, R.string.settings_theme_light),
    ThemeOption(ThemeMode.Dark, R.string.settings_theme_dark),
)

/**
 * Mockup "Настройки · плеер и оформление": two player cards, theme and language chips, OK picks one.
 * [selected] and [themeMode] are null while the settings load.
 */
@Composable
fun SettingsScreen(
    selected: PlayerEngine?,
    onPlayerSelected: (PlayerEngine) -> Unit,
    themeMode: ThemeMode?,
    onThemeSelected: (ThemeMode) -> Unit,
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.HomeSectionSpacing),
        modifier = modifier
            .fillMaxSize()
            .padding(start = TwiDimens.ContentStartPadding, end = TwiDimens.ContentEndPadding, top = TwiDimens.ScreenTopPadding),
    ) {
        Text(stringResource(R.string.settings_title), style = TwiTheme.typography.screenTitle, color = TwiTheme.colors.onSurface)
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
        ThemeSection(themeMode, onThemeSelected)
        LanguageSection(language, onLanguageSelected)
    }
}

@Composable
private fun LanguageSection(language: AppLanguage, onLanguageSelected: (AppLanguage) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.HomeRowTitleSpacing)) {
        Text(stringResource(R.string.settings_language), style = TwiTheme.typography.rowTitleDense, color = TwiTheme.colors.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(TwiDimens.ChipSpacing)) {
            LanguageOptions.forEach { option ->
                TwiChip(stringResource(option.title), selected = option.language == language, onClick = { onLanguageSelected(option.language) })
            }
        }
    }
}

@Composable
private fun ThemeSection(themeMode: ThemeMode?, onThemeSelected: (ThemeMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.HomeRowTitleSpacing)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(TwiDimens.HomeRowTitleSpacing)) {
            Text(stringResource(R.string.settings_theme), style = TwiTheme.typography.rowTitleDense, color = TwiTheme.colors.onSurface)
            Text(stringResource(R.string.settings_theme_hint), style = TwiTheme.typography.caption, color = TwiTheme.colors.onSurfaceVariant)
        }
        if (themeMode != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(TwiDimens.ChipSpacing)) {
                ThemeOptions.forEach { option ->
                    TwiChip(stringResource(option.title), selected = option.mode == themeMode, onClick = { onThemeSelected(option.mode) })
                }
            }
        }
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
            // Opening the screen lands on the current choice.
            .then(if (selected) Modifier.requestInitialFocus() else Modifier)
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
