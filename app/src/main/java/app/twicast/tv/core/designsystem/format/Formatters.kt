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

private fun oneDecimal(value: Double): String =
    DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.getDefault())).format(value)
