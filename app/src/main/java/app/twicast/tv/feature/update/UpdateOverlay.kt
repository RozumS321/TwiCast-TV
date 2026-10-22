package app.twicast.tv.feature.update

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.TwiPrimaryButton
import app.twicast.tv.core.designsystem.component.TwiSecondaryButton
import app.twicast.tv.core.designsystem.component.requestInitialFocus
import app.twicast.tv.core.designsystem.icon.TwiIcons
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.update.AppRelease
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private const val BytesInMegabyte = 1024f * 1024f
private const val Percent = 100

/** What the update panel can ask for; one interface keeps the composables' parameter lists short. */
interface UpdateActions {
    fun onUpdate()
    fun onCancelDownload()
    fun onOpenPermissionSettings()
    fun onDismiss()
}

/** Long releases: a few lines per section, the rest summed up as "и ещё N". */
private const val MaxNotesPerSection = 4
private const val MaxNoteLines = 2

/**
 * Modal panel over the whole app (mockups "Обновление · …"). Focus stays inside it; Back dismisses
 * (or cancels the download).
 */
@Composable
fun UpdateOverlay(state: UpdateUiState, actions: UpdateActions, modifier: Modifier = Modifier) {
    if (state == UpdateUiState.Hidden) return
    BackHandler { if (state is UpdateUiState.Downloading) actions.onCancelDownload() else actions.onDismiss() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(TwiTheme.colors.scrim)
            .padding(vertical = TwiDimens.UpdatePanelScreenMargin),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(TwiDimens.UpdatePanelSpacing),
            modifier = Modifier
                .width(TwiDimens.UpdatePanelWidth)
                .graphicsLayer {
                    shape = TwiShapes.Panel
                    shadowElevation = TwiDimens.LoginPanelShadowElevation.toPx()
                }
                .background(TwiTheme.colors.surface, TwiShapes.Panel)
                // The app behind stays visible but unreachable while the panel is up.
                .focusProperties { onExit = { cancelFocusChange() } }
                .focusGroup()
                .padding(horizontal = TwiDimens.UpdatePanelHorizontalPadding, vertical = TwiDimens.UpdatePanelVerticalPadding),
        ) {
            UpdateHeader(state)
            // Notes take only the room left: the buttons below always stay on screen, however long the release.
            state.release()?.let { NotesCards(it, Modifier.weight(1f, fill = false)) }
            if (state is UpdateUiState.Downloading) DownloadProgress(state)
            UpdateButtons(state, actions)
        }
    }
}

@Composable
private fun UpdateHeader(state: UpdateUiState) {
    val release = state.release() ?: return
    val whatsNew = state is UpdateUiState.WhatsNew
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwiDimens.UpdateHeaderSpacing)) {
        Image(painterResource(R.drawable.ic_logo_mark), contentDescription = null, modifier = Modifier.size(TwiDimens.UpdateLogoSize))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TwiDimens.CardTextSpacing * 2)) {
            Text(
                text = stringResource(
                    when (state) {
                        is UpdateUiState.WhatsNew -> R.string.update_overline_whats_new
                        is UpdateUiState.Downloading -> R.string.update_overline_downloading
                        else -> R.string.update_overline_available
                    },
                ).uppercase(),
                style = TwiTheme.typography.overline,
                color = TwiTheme.colors.primaryStrong,
            )
            Text(
                text = if (whatsNew) {
                    stringResource(R.string.update_title_installed, release.versionName)
                } else {
                    stringResource(R.string.update_title, release.versionName)
                },
                style = TwiTheme.typography.greeting,
                color = TwiTheme.colors.onSurface,
            )
            Text(
                text = if (whatsNew) {
                    stringResource(R.string.update_subtitle_installed)
                } else {
                    stringResource(R.string.update_subtitle, state.installedVersion().orEmpty(), megabytes(release.apkSizeBytes))
                },
                style = TwiTheme.typography.bodySmall,
                color = TwiTheme.colors.onSurfaceVariant,
            )
        }
        if (whatsNew) InstalledBadge()
    }
}

@Composable
private fun InstalledBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.KeySpacing),
        modifier = Modifier
            .background(TwiTheme.colors.primaryContainer, TwiShapes.Pill)
            .padding(horizontal = TwiDimens.BadgePillHorizontalPadding, vertical = TwiDimens.BadgePillVerticalPadding),
    ) {
        Icon(TwiIcons.Check, contentDescription = null, tint = TwiTheme.colors.primaryStrong, modifier = Modifier.size(TwiDimens.NotesBadgeIconSize))
        Text(stringResource(R.string.update_installed_badge), style = TwiTheme.typography.chip, color = TwiTheme.colors.primaryStrong)
    }
}

/** "Новое / Исправлено / Улучшено" side by side; empty sections are left out. */
@Composable
private fun NotesCards(release: AppRelease, modifier: Modifier) {
    val colors = TwiTheme.colors
    val sections = listOf(
        NotesSection(stringResource(R.string.update_section_added), TwiIcons.Plus, colors.primary, release.notes.added),
        NotesSection(stringResource(R.string.update_section_fixed), TwiIcons.Check, colors.success, release.notes.fixed),
        NotesSection(stringResource(R.string.update_section_improved), TwiIcons.ArrowUp, colors.info, release.notes.improved),
    ).filter { it.items.isNotEmpty() }
    if (sections.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.NotesCardSpacing),
        // Cards as tall as the tallest one, not as tall as the space allowed.
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
    ) {
        sections.forEach { section -> NotesCard(section, Modifier.weight(1f).fillMaxHeight()) }
    }
}

private data class NotesSection(val title: String, val icon: ImageVector, val color: Color, val items: List<String>)

@Composable
private fun NotesCard(section: NotesSection, modifier: Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.NotesItemSpacing),
        modifier = modifier
            .background(TwiTheme.colors.background, TwiShapes.NotesCard)
            .padding(horizontal = TwiDimens.NotesCardHorizontalPadding, vertical = TwiDimens.NotesCardVerticalPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwiDimens.NotesItemSpacing)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(TwiDimens.NotesBadgeSize).background(section.color, CircleShape),
            ) {
                Icon(section.icon, contentDescription = null, tint = TwiTheme.colors.onPrimary, modifier = Modifier.size(TwiDimens.NotesBadgeIconSize))
            }
            Text(section.title, style = TwiTheme.typography.listTitle, color = TwiTheme.colors.onSurface)
        }
        section.items.take(MaxNotesPerSection).forEach { item ->
            Text(
                item,
                style = TwiTheme.typography.bodySmall,
                color = TwiTheme.colors.onSurfaceSecondary,
                maxLines = MaxNoteLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val hidden = section.items.size - MaxNotesPerSection
        if (hidden > 0) {
            Text(
                pluralStringResource(R.plurals.update_more_items, hidden, hidden),
                style = TwiTheme.typography.bodySmall,
                color = TwiTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DownloadProgress(state: UpdateUiState.Downloading) {
    val fraction = if (state.totalBytes > 0) (state.downloadedBytes.toFloat() / state.totalBytes).coerceIn(0f, 1f) else 0f
    val primary = TwiTheme.colors.primary
    Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.NotesItemSpacing)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(TwiDimens.ProgressHeight)
                .background(TwiTheme.colors.surfaceVariant, TwiShapes.Pill),
        ) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(primary, TwiShapes.Pill))
        }
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.update_downloading), style = TwiTheme.typography.bodySmall, color = TwiTheme.colors.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(
                    R.string.update_progress,
                    megabytes(state.downloadedBytes),
                    megabytes(state.totalBytes),
                    (fraction * Percent).toInt(),
                ),
                style = TwiTheme.typography.bodySmall,
                color = TwiTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UpdateButtons(state: UpdateUiState, actions: UpdateActions) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwiDimens.ButtonSpacing)) {
        when (state) {
            is UpdateUiState.Available -> {
                TwiPrimaryButton(stringResource(R.string.update_action_update), actions::onUpdate, Modifier.requestInitialFocus())
                TwiSecondaryButton(stringResource(R.string.update_action_later), actions::onDismiss)
            }
            is UpdateUiState.Downloading ->
                TwiSecondaryButton(stringResource(R.string.update_action_cancel), actions::onCancelDownload, Modifier.requestInitialFocus())
            is UpdateUiState.Failed -> {
                TwiPrimaryButton(stringResource(R.string.update_action_retry), actions::onUpdate, Modifier.requestInitialFocus())
                TwiSecondaryButton(stringResource(R.string.update_action_later), actions::onDismiss)
            }
            is UpdateUiState.NeedsPermission -> {
                TwiPrimaryButton(stringResource(R.string.update_action_allow), actions::onOpenPermissionSettings, Modifier.requestInitialFocus())
                TwiSecondaryButton(stringResource(R.string.update_action_later), actions::onDismiss)
            }
            is UpdateUiState.WhatsNew ->
                TwiPrimaryButton(stringResource(R.string.update_action_ok), actions::onDismiss, Modifier.requestInitialFocus())
            UpdateUiState.Hidden -> Unit
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(
                when (state) {
                    is UpdateUiState.Downloading -> R.string.update_footnote_downloading
                    is UpdateUiState.Failed -> R.string.update_footnote_failed
                    is UpdateUiState.NeedsPermission -> R.string.update_footnote_permission
                    is UpdateUiState.WhatsNew -> R.string.update_footnote_installed
                    else -> R.string.update_footnote_available
                },
            ),
            style = TwiTheme.typography.caption,
            color = TwiTheme.colors.onSurfaceVariant,
        )
    }
}

private fun UpdateUiState.release(): AppRelease? = when (this) {
    is UpdateUiState.Available -> release
    is UpdateUiState.Downloading -> release
    is UpdateUiState.NeedsPermission -> release
    is UpdateUiState.Failed -> release
    is UpdateUiState.WhatsNew -> release
    UpdateUiState.Hidden -> null
}

private fun UpdateUiState.installedVersion(): String? = when (this) {
    is UpdateUiState.Available -> installedVersion
    is UpdateUiState.Downloading -> installedVersion
    is UpdateUiState.NeedsPermission -> installedVersion
    is UpdateUiState.Failed -> installedVersion
    is UpdateUiState.WhatsNew, UpdateUiState.Hidden -> null
}

@Composable
private fun megabytes(bytes: Long): String {
    val value = DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.getDefault())).format(bytes / BytesInMegabyte)
    return stringResource(R.string.update_megabytes, value)
}
