package app.twicast.tv.core.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.twicast.tv.R
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val Thousand = 1_000
private const val Million = 1_000_000

/** Compact count as on the mockups: 980, 9,8 тыс, 1,2 млн. */
@Composable
@ReadOnlyComposable
fun formatViewerCount(count: Int): String = when {
    count < Thousand -> count.toString()
    count < Million -> stringResource(R.string.count_thousands, oneDecimal(count.toDouble() / Thousand))
    else -> stringResource(R.string.count_millions, oneDecimal(count.toDouble() / Million))
}

/** "18,2 тыс зрителей" — the plural form follows the full number. */
@Composable
@ReadOnlyComposable
fun formatViewersLabel(count: Int): String =
    pluralStringResource(R.plurals.viewers, count, formatViewerCount(count))

/** "3 ч в эфире" / "25 мин в эфире". */
@Composable
@ReadOnlyComposable
fun formatUptime(startedAtMillis: Long, nowMillis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes((nowMillis - startedAtMillis).coerceAtLeast(0))
    val hours = TimeUnit.MINUTES.toHours(minutes)
    return if (hours > 0) {
        stringResource(R.string.uptime_hours, hours)
    } else {
        stringResource(R.string.uptime_minutes, minutes)
    }
}

/** Hours and minutes a stream has been on: (2, 14). A start in the future (clock skew) counts as just started. */
fun uptimeParts(startedAtMillis: Long, nowMillis: Long): Pair<Long, Long> {
    val minutes = TimeUnit.MILLISECONDS.toMinutes((nowMillis - startedAtMillis).coerceAtLeast(0))
    return TimeUnit.MINUTES.toHours(minutes) to minutes % SecondsPerMinute
}

/** The card's uptime badge: "2 ч" from an hour on, "35 мин" before. */
@Composable
@ReadOnlyComposable
fun formatUptimeShort(startedAtMillis: Long, nowMillis: Long): String {
    val (hours, minutes) = uptimeParts(startedAtMillis, nowMillis)
    return if (hours > 0) stringResource(R.string.uptime_short_hours, hours) else stringResource(R.string.uptime_short_minutes, minutes)
}

/** A recording's clock: "1:24:10", "4:05" under an hour. */
fun formatPlaybackTime(millis: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis.coerceAtLeast(0))
    val hours = totalSeconds / SecondsPerHour
    val minutes = totalSeconds % SecondsPerHour / SecondsPerMinute
    val seconds = totalSeconds % SecondsPerMinute
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

/** The dialled jump over the seek bar: "+ 10 с", "− 1 мин", "+ 2 мин 30 с". */
@Composable
@ReadOnlyComposable
fun formatSeekDelta(millis: Long): String {
    val sign = if (millis < 0) "−" else "+"
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(kotlin.math.abs(millis))
    val minutes = totalSeconds / SecondsPerMinute
    val seconds = totalSeconds % SecondsPerMinute
    return when {
        minutes == 0L -> stringResource(R.string.seek_delta_seconds, sign, seconds)
        seconds == 0L -> stringResource(R.string.seek_delta_minutes, sign, minutes)
        else -> stringResource(R.string.seek_delta_minutes_seconds, sign, minutes, seconds)
    }
}

private const val SecondsPerMinute = 60L
private const val SecondsPerHour = 3_600L

private fun oneDecimal(value: Double): String =
    DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.getDefault())).format(value)
