package app.twicast.tv.feature.changelog

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.component.ErrorState
import app.twicast.tv.core.designsystem.component.KeepFocusedVisibleScroll
import app.twicast.tv.core.designsystem.component.SkeletonBlock
import app.twicast.tv.core.designsystem.component.TwiPrimaryButton
import app.twicast.tv.core.designsystem.component.focusRing
import app.twicast.tv.core.designsystem.component.rememberFocusProgress
import app.twicast.tv.core.designsystem.component.rememberSkeletonPulse
import app.twicast.tv.core.designsystem.component.requestInitialFocus
import app.twicast.tv.core.designsystem.component.trackFocus
import app.twicast.tv.core.designsystem.icon.TwiIcons
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.update.AppRelease
import app.twicast.tv.feature.common.appErrorMessage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val SkeletonCards = 3

/**
 * Mockup "Что нового · история обновлений": every release by day, newest first. The remote moves the focus from
 * card to card and the list follows; a thin bar on the right shows where in the history the viewer is.
 */
@Composable
fun ChangelogScreen(
    state: ChangelogUiState,
    onRetry: () -> Unit,
    onUpdate: (AppRelease) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.SettingsContentSpacing),
        modifier = modifier
            .fillMaxSize()
            .padding(start = TwiDimens.ContentStartPadding, end = TwiDimens.ContentEndPadding, top = TwiDimens.ScreenTopPadding),
    ) {
        Header(state, onUpdate)
        when (state) {
            ChangelogUiState.Loading -> ChangelogSkeleton()
            is ChangelogUiState.Error -> ErrorState(appErrorMessage(state.error), onRetry, Modifier.fillMaxSize())
            is ChangelogUiState.Content -> KeepFocusedVisibleScroll { ReleaseList(state, focusFirst = state.newer == null) }
        }
    }
}

@Composable
private fun Header(state: ChangelogUiState, onUpdate: (AppRelease) -> Unit) {
    val content = state as? ChangelogUiState.Content
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TwiDimens.ButtonSpacing)) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(TwiDimens.HomeHeaderSpacing),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                stringResource(R.string.changelog_title),
                style = TwiTheme.typography.screenTitle,
                color = TwiTheme.colors.onSurface,
                modifier = Modifier.alignByBaseline(),
            )
            if (content != null) {
                Text(
                    stringResource(R.string.changelog_installed, content.installedVersion),
                    style = TwiTheme.typography.body,
                    color = TwiTheme.colors.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        val newer = content?.newer
        if (newer != null) {
            TwiPrimaryButton(
                text = stringResource(R.string.changelog_update, newer.versionName),
                onClick = { onUpdate(newer) },
                icon = TwiIcons.ArrowUp,
                modifier = Modifier.requestInitialFocus(),
            )
        }
    }
}

@Composable
private fun ReleaseList(state: ChangelogUiState.Content, focusFirst: Boolean) {
    val listState = rememberLazyListState()
    Row(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(TwiDimens.ChangelogCardSpacing),
            contentPadding = PaddingValues(top = TwiDimens.FocusBorderWidth, bottom = TwiDimens.ScreenTopPadding),
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            state.days.forEachIndexed { dayIndex, day ->
                item(key = "day-${day.date}", contentType = "day") { DayTitle(day.date) }
                items(day.releases, key = { it.versionCode }, contentType = { "release" }) { release ->
                    ReleaseCard(
                        release = release,
                        installed = release.versionCode == state.installedCode,
                        modifier = if (focusFirst && dayIndex == 0 && release == day.releases.first()) Modifier.requestInitialFocus() else Modifier,
                    )
                }
            }
        }
        ScrollIndicator(listState, Modifier.padding(start = TwiDimens.ChangelogIndicatorGap).fillMaxHeight())
    }
}

/** "Сегодня, 24 октября" / "Вчера, 23 октября" / "22 октября", in the TV's time zone. */
@Composable
private fun DayTitle(date: LocalDate?) {
    val text = when (date) {
        null -> stringResource(R.string.changelog_undated)
        else -> {
            val formatted = DateTimeFormatter.ofPattern("d MMMM", locale()).format(date)
            val today = LocalDate.now(ZoneId.systemDefault())
            when (date) {
                today -> stringResource(R.string.changelog_today, formatted)
                today.minusDays(1) -> stringResource(R.string.changelog_yesterday, formatted)
                else -> formatted
            }
        }
    }
    Text(
        text,
        style = TwiTheme.typography.rowTitleDense,
        color = TwiTheme.colors.onSurfaceSecondary,
        modifier = Modifier.padding(top = TwiDimens.ChangelogDaySpacing),
    )
}

@Composable
@ReadOnlyComposable
private fun locale() = LocalConfiguration.current.locales[0]

/** One release: number, time, "у вас" on the installed one; on the right its notes by section. Focusable to scroll. */
@Composable
private fun ReleaseCard(release: AppRelease, installed: Boolean, modifier: Modifier = Modifier) {
    val colors = TwiTheme.colors
    val focusProgress = rememberFocusProgress()
    Row(
        horizontalArrangement = Arrangement.spacedBy(TwiDimens.ChangelogCardGap),
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, TwiShapes.Card)
            .focusRing(focusProgress, colors.primary, TwiShapes.Card, TwiDimens.FocusBorderWidth)
            .trackFocus(focusProgress)
            .focusable()
            .padding(horizontal = TwiDimens.ChangelogCardHorizontalPadding, vertical = TwiDimens.ChangelogCardVerticalPadding),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.CardTextSpacing), modifier = Modifier.width(TwiDimens.ChangelogVersionWidth)) {
            Text(release.versionName, style = TwiTheme.typography.listTitle, color = colors.onSurface)
            release.publishedAtMillis?.let { published ->
                Text(
                    DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(published).atZone(ZoneId.systemDefault())),
                    style = TwiTheme.typography.caption,
                    color = colors.onSurfaceVariant,
                )
            }
            if (installed) {
                Text(
                    stringResource(R.string.changelog_installed_badge),
                    style = TwiTheme.typography.chip,
                    color = colors.primaryStrong,
                    modifier = Modifier
                        .background(colors.primaryContainer, TwiShapes.Pill)
                        .padding(horizontal = TwiDimens.BadgePillHorizontalPadding, vertical = TwiDimens.BadgePillVerticalPadding),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.ChangelogSectionSpacing), modifier = Modifier.weight(1f)) {
            NotesGroup(stringResource(R.string.update_section_added), colors.primary, release.notes.added)
            NotesGroup(stringResource(R.string.update_section_fixed), colors.success, release.notes.fixed)
            NotesGroup(stringResource(R.string.update_section_improved), colors.info, release.notes.improved)
        }
    }
}

@Composable
private fun NotesGroup(title: String, color: Color, items: List<String>) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.CardTextSpacing)) {
        Text(title.uppercase(locale()), style = TwiTheme.typography.overline, color = color)
        items.forEach { Text("• $it", style = TwiTheme.typography.body, color = TwiTheme.colors.onSurfaceSecondary) }
    }
}

/** Where in the history the viewer is: a thumb on a thin track, drawn from the list's scroll in the draw phase. */
@Composable
private fun ScrollIndicator(listState: LazyListState, modifier: Modifier) {
    val track = TwiTheme.colors.outlineVariant
    val thumb = TwiTheme.colors.primary
    Box(
        modifier
            .width(TwiDimens.ChangelogIndicatorWidth)
            .drawBehind {
                val info = listState.layoutInfo
                val total = info.totalItemsCount
                val visible = info.visibleItemsInfo.size
                if (total == 0 || visible >= total) return@drawBehind
                val radius = CornerRadius(size.width / 2)
                drawRoundRect(track, cornerRadius = radius)
                val thumbHeight = (size.height * visible / total).coerceAtLeast(size.width * 2)
                val progress = listState.firstVisibleItemIndex.toFloat() / (total - visible).coerceAtLeast(1)
                val top = (size.height - thumbHeight) * progress.coerceIn(0f, 1f)
                drawRoundRect(thumb, topLeft = Offset(0f, top), size = Size(size.width, thumbHeight), cornerRadius = radius)
            },
    )
}

@Composable
private fun ChangelogSkeleton() {
    val pulse = rememberSkeletonPulse()
    Column(verticalArrangement = Arrangement.spacedBy(TwiDimens.ChangelogCardSpacing)) {
        repeat(SkeletonCards) {
            SkeletonBlock(pulse, TwiShapes.Card, Modifier.fillMaxWidth().height(TwiDimens.ChangelogSkeletonHeight))
        }
    }
}
