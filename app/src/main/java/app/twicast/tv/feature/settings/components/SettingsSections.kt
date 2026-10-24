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
import androidx.compose.ui.unit.Dp
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

/** "Уведомления": "Сообщать о начале эфиров" — system notifications, checked in the background. */
@Composable
fun NotificationsSection(notify: Boolean?, onNotifyChanged: (Boolean) -> Unit) {
    SettingsSection(R.string.settings_notifications, hint = null) {
        if (notify == null) return@SettingsSection
        SwitchCard(
            title = stringResource(R.string.settings_notify_live),
            text = stringResource(R.string.settings_notify_live_hint),
            checked = notify,
            width = TwiDimens.SettingsWideToggleWidth,
            onToggle = { onNotifyChanged(!notify) },
        )
    }
}

/** "Чат": a card-wide switch (mockup), OK flips it. Hidden while the setting loads. */
@Composable
fun ChatSection(openChat: Boolean?, onOpenChatChanged: (Boolean) -> Unit) {
    SettingsSection(R.string.settings_chat, hint = null) {
        if (openChat == null) return@SettingsSection
        SwitchCard(
            title = stringResource(R.string.settings_chat_open),
            text = stringResource(R.string.settings_chat_open_hint),
            checked = openChat,
            onToggle = { onOpenChatChanged(!openChat) },
        )
    }
}

/** Memory a warm Twitch player keeps (its WebView renderer), measured on a 4 GB TV box. */
private const val WarmPlayerCostMb = 250

/**
 * Mockup "Настройки · Оптимизация": what the app may trade. [warmAvailable]: the Twitch player is chosen;
 * [lowMemory]: the device is too small for a warm player at all.
 */
@Composable
fun OptimizationSections(
    warmPlayer: Boolean?,
    warmAvailable: Boolean,
    lowMemory: Boolean,
    freeMemory: Boolean?,
    onWarmPlayerChanged: (Boolean) -> Unit,
    onFreeMemoryChanged: (Boolean) -> Unit,
) {
    SettingsSection(R.string.settings_speed, R.string.settings_speed_hint) {
        if (warmPlayer == null) return@SettingsSection
        val enabled = warmAvailable && !lowMemory
        SwitchCard(
            title = stringResource(R.string.settings_warm_player),
            text = stringResource(R.string.settings_warm_player_text),
            tag = stringResource(R.string.settings_warm_player_cost, WarmPlayerCostMb),
            checked = warmPlayer && enabled,
            enabled = enabled,
            note = when {
                lowMemory -> stringResource(R.string.settings_warm_player_low_memory)
                !warmAvailable -> stringResource(R.string.settings_warm_player_twitch_only)
                else -> null
            },
            width = TwiDimens.SettingsWideToggleWidth,
            onToggle = { onWarmPlayerChanged(!warmPlayer) },
        )
    }
    SettingsSection(R.string.settings_memory, R.string.settings_memory_hint) {
        if (freeMemory == null) return@SettingsSection
        SwitchCard(
            title = stringResource(R.string.settings_free_memory),
            text = stringResource(R.string.settings_free_memory_text),
            checked = freeMemory,
            width = TwiDimens.SettingsWideToggleWidth,
            onToggle = { onFreeMemoryChanged(!freeMemory) },
        )
    }
}

/**
 * A card-wide switch: title (with an optional [tag] such as "≈ 250 МБ"), explanation, optional [note] under it.
 * A disabled card is dimmed and skipped by the remote.
 */
@Composable
private fun SwitchCard(
    title: String,
    text: String,
    checked: Boolean,
    onToggle: () -> Unit,
    tag: String? = null,
    enabled: Boolean = true,
    note: String? = null,
    width: Dp = TwiDimens.SettingsToggleWidth,
) {
    val colors = TwiTheme.colors
    val focusProgress = rememberFocusProgress()
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.SettingsSectionsSpacing),
        modifier = Modifier
            .width(width)
            .graphicsLayer { alpha = if (enabled) 1f else DisabledAlpha }
            .focusTransform(focusProgress, TwiScale.ButtonFocused, TwiShapes.SettingsToggle)
            .background(colors.surface, TwiShapes.SettingsToggle)
            .focusRing(focusProgress, colors.primary, TwiShapes.SettingsToggle, TwiDimens.FocusBorderWidth)
            .trackFocus(focusProgress)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onToggle)
            .padding(horizontal = TwiDimens.SettingsToggleHorizontalPadding, vertical = TwiDimens.SettingsToggleVerticalPadding),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TwiDimens.CardTextSpacing)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwiDimens.ChipSpacing)) {
                Text(title, style = TwiTheme.typography.listTitle, color = colors.onSurface)
                if (tag != null) {
                    Text(
                        tag,
                        style = TwiTheme.typography.tagDense,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier
                            .background(colors.surfaceVariant, TwiShapes.Pill)
                            .padding(horizontal = TwiDimens.BadgePillHorizontalPadding, vertical = TwiDimens.BadgePillVerticalPadding),
                    )
                }
            }
            Text(text, style = TwiTheme.typography.caption, color = colors.onSurfaceVariant)
            if (note != null) Text(note, style = TwiTheme.typography.caption, color = colors.primaryStrong)
        }
        Switch(checked = checked)
    }
}

private const val DisabledAlpha = 0.55f

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
