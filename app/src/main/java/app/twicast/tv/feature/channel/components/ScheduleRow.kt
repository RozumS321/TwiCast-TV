package app.twicast.tv.feature.channel.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Text
import app.twicast.tv.R
import app.twicast.tv.core.designsystem.theme.TwiDimens
import app.twicast.tv.core.designsystem.theme.TwiShapes
import app.twicast.tv.core.designsystem.theme.TwiTheme
import app.twicast.tv.data.model.ScheduledStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** How a scheduled day reads: by name for today and tomorrow, else by date. */
enum class ScheduleDay { Today, Tomorrow, Later }

/** Which day [startMillis] falls on, seen from [nowMillis], in the viewer's [zone]. */
fun scheduleDay(startMillis: Long, nowMillis: Long, zone: ZoneId): ScheduleDay {
    val day = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    return when (day) {
        today -> ScheduleDay.Today
        today.plusDays(1) -> ScheduleDay.Tomorrow
        else -> ScheduleDay.Later
    }
}

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
@ReadOnlyComposable
private fun locale(): Locale = LocalConfiguration.current.locales[0]

/** "Сегодня", "Завтра", "Пн 27 окт". */
@Composable
@ReadOnlyComposable
private fun dayLabel(startMillis: Long): String {
    val zone = ZoneId.systemDefault()
    return when (scheduleDay(startMillis, System.currentTimeMillis(), zone)) {
        ScheduleDay.Today -> stringResource(R.string.schedule_today)
        ScheduleDay.Tomorrow -> stringResource(R.string.schedule_tomorrow)
        ScheduleDay.Later -> DateTimeFormatter.ofPattern("EEE d MMM", locale()).format(Instant.ofEpochMilli(startMillis).atZone(zone))
            .replace(".", "")
            .replaceFirstChar { it.titlecase(locale()) }
    }
}

private fun timeOf(millis: Long): String = TimeFormat.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

/** The offline status line: "следующий эфир завтра в 18:00". */
@Composable
@ReadOnlyComposable
fun nextStreamLabel(stream: ScheduledStream): String =
    stringResource(R.string.channel_next_stream, dayLabel(stream.startMillis).lowercase(locale()), timeOf(stream.startMillis))

/**
 * One upcoming stream (mockup "Канал · расписание"): "Завтра · 18:00", "до 22:00 · Counter-Strike", its title.
 * Information only: it takes no focus. [next]: the soonest one, tinted.
 */
@Composable
fun ScheduleCard(stream: ScheduledStream, next: Boolean, modifier: Modifier = Modifier) {
    val colors = TwiTheme.colors
    val until = listOfNotNull(
        stream.endMillis?.let { stringResource(R.string.schedule_until, timeOf(it)) },
        stream.categoryName.ifEmpty { null },
    ).joinToString(" · ")
    Column(
        verticalArrangement = Arrangement.spacedBy(TwiDimens.ScheduleCardSpacing),
        modifier = modifier
            .width(TwiDimens.ScheduleCardWidth)
            .background(if (next) colors.primaryContainer else colors.surface, TwiShapes.Card)
            .border(TwiDimens.OutlineWidth, colors.outlineVariant, TwiShapes.Card)
            .padding(horizontal = TwiDimens.ScheduleCardHorizontalPadding, vertical = TwiDimens.ScheduleCardVerticalPadding),
    ) {
        Text(
            stringResource(R.string.schedule_when, dayLabel(stream.startMillis), timeOf(stream.startMillis)),
            style = TwiTheme.typography.bodyStrong,
            color = colors.onSurface,
            maxLines = 1,
        )
        if (until.isNotEmpty()) {
            Text(until, style = TwiTheme.typography.caption, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (stream.title.isNotBlank()) {
            Text(stream.title, style = TwiTheme.typography.caption, color = colors.onSurfaceSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
