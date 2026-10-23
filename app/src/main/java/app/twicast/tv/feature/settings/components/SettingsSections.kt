package app.twicast.tv.feature.settings.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
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
import app.twicast.tv.data.model.ThemeMode
import app.twicast.tv.feature.settings.DefaultQuality

private class ThemeOption(val mode: ThemeMode, val title: Int)

private val ThemeOptions = listOf(
    ThemeOption(ThemeMode.System, R.string.settings_theme_system),
    ThemeOption(ThemeMode.Light, R.string.settings_theme_light),
    ThemeOption(ThemeMode.Dark, R.string.settings_theme_dark),
)

private class LanguageOption(val language: AppLanguage, val title: Int)

private val LanguageOptions = listOf(
    LanguageOption(AppLanguage.Russian, R.string.settings_language_russian),
    LanguageOption(AppLanguage.Ukrainian, R.string.settings_language_ukrainian),
    LanguageOption(AppLanguage.English, R.string.settings_language_english),
)

/** "Качество по умолчанию": Авто, then height ceilings. Nothing is shown while the setting loads. */
@Composable
fun QualitySection(selected: DefaultQuality?, loaded: Boolean, onSelected: (DefaultQuality) -> Unit) {
    SettingsSection(R.string.settings_quality, R.string.settings_quality_hint) {
        if (!loaded) return@SettingsSection
        Row(horizontalArrangement = Arrangement.spacedBy(TwiDimens.ChipSpacing)) {
            DefaultQuality.entries.forEach { quality ->
                val label = if (quality == DefaultQuality.Auto) stringResource(R.string.player_quality_auto) else quality.id
                TwiChip(label, selected = quality == selected, onClick = { onSelected(quality) })
            }
        }
    }
}

/** "Оформление": System / Light / Dark. Hidden while the setting loads. */
@Composable
fun ThemeSection(themeMode: ThemeMode?, onThemeSelected: (ThemeMode) -> Unit) {
    SettingsSection(R.string.settings_theme, R.string.settings_theme_hint) {
        if (themeMode == null) return@SettingsSection
        Row(horizontalArrangement = Arrangement.spacedBy(TwiDimens.ChipSpacing)) {
            ThemeOptions.forEach { option ->
                TwiChip(stringResource(option.title), selected = option.mode == themeMode, onClick = { onThemeSelected(option.mode) })
            }
        }
    }
}

/** "Язык": each language named in itself. [focusLanguage]: just redrawn in a new language, focus stays here. */
@Composable
fun LanguageSection(language: AppLanguage, focusLanguage: Boolean, onLanguageSelected: (AppLanguage) -> Unit) {
    SettingsSection(R.string.settings_language, R.string.settings_language_hint) {
        Row(horizontalArrangement = Arrangement.spacedBy(TwiDimens.ChipSpacing)) {
            LanguageOptions.forEach { option ->
                val isCurrent = option.language == language
                TwiChip(
                    text = stringResource(option.title),
                    selected = isCurrent,
                    onClick = { onLanguageSelected(option.language) },
                    modifier = if (isCurrent && focusLanguage) Modifier.requestInitialFocus() else Modifier,
                )
            }
        }
    }
}

/** "О приложении": version; updates arrive by themselves, so there is nothing to press. */
@Composable
fun AboutSection(versionName: String) {
    SettingsSection(R.string.settings_about, hint = null) {
        val colors = TwiTheme.colors
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TwiDimens.ChipSpacing),
            modifier = Modifier
                .width(TwiDimens.SettingsToggleWidth)
                .background(colors.surface, TwiShapes.SettingsToggle)
                .padding(horizontal = TwiDimens.SettingsToggleHorizontalPadding, vertical = TwiDimens.SettingsToggleVerticalPadding),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo_mark),
                contentDescription = null,
                modifier = Modifier.size(TwiDimens.LogoMarkSize),
            )
            Column {
                Text(stringResource(R.string.profile_version, versionName), style = TwiTheme.typography.listTitle, color = colors.onSurface)
                Text(stringResource(R.string.settings_about_hint), style = TwiTheme.typography.caption, color = colors.onSurfaceVariant)
            }
        }
    }
}

/** "Чат": a card-wide switch (mockup), OK flips it. Hidden while the setting loads. */
@Composable
fun ChatSection(openChat: Boolean?, onOpenChatChanged: (Boolean) -> Unit) {
    SettingsSection(R.string.settings_chat, hint = null) {
        if (openChat == null) return@SettingsSection
        val colors = TwiTheme.colors
        val focusProgress = rememberFocusProgress()
        val interactionSource = remember { MutableInteractionSource() }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TwiDimens.ChipSpacing),
            modifier = Modifier
                .width(TwiDimens.SettingsToggleWidth)
                .focusTransform(focusProgress, TwiScale.ButtonFocused, TwiShapes.SettingsToggle)
                .background(colors.surface, TwiShapes.SettingsToggle)
                .focusRing(focusProgress, colors.primary, TwiShapes.SettingsToggle, TwiDimens.FocusBorderWidth)
                .trackFocus(focusProgress)
                .clickable(interactionSource = interactionSource, indication = null) { onOpenChatChanged(!openChat) }
                .padding(horizontal = TwiDimens.SettingsToggleHorizontalPadding, vertical = TwiDimens.SettingsToggleVerticalPadding),
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_chat_open), style = TwiTheme.typography.listTitle, color = colors.onSurface)
                Text(stringResource(R.string.settings_chat_open_hint), style = TwiTheme.typography.caption, color = colors.onSurfaceVariant)
            }
            Switch(checked = openChat)
        }
    }
}

@Composable
private fun Switch(checked: Boolean) {
    val colors = TwiTheme.colors
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .size(TwiDimens.SettingsSwitchWidth, TwiDimens.SettingsSwitchHeight)
            .background(if (checked) colors.primary else colors.outline, CircleShape),
    ) {
        Box(
            Modifier
                // The knob moves in the draw phase: no relayout when the switch flips.
                .graphicsLayer {
                    val travel = (TwiDimens.SettingsSwitchWidth - TwiDimens.SettingsSwitchKnob - TwiDimens.SettingsSwitchKnobInset * 2).toPx()
                    translationX = TwiDimens.SettingsSwitchKnobInset.toPx() + if (checked) travel else 0f
                }
                .size(TwiDimens.SettingsSwitchKnob)
                .background(colors.onPrimary, CircleShape),
        )
    }
}

/** Section title with an optional hint on the same baseline, then its controls. */
@Composable
private fun SettingsSection(title: Int, hint: Int?, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.HomeRowTitleSpacing)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(TwiDimens.HomeRowTitleSpacing)) {
            Text(stringResource(title), style = TwiTheme.typography.rowTitleDense, color = TwiTheme.colors.onSurface)
            if (hint != null) {
                Text(stringResource(hint), style = TwiTheme.typography.caption, color = TwiTheme.colors.onSurfaceVariant)
            }
        }
        content()
    }
}
