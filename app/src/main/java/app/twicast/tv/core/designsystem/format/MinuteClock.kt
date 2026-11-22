package app.twicast.tv.core.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import kotlinx.coroutines.delay

/**
 * "Now" for the "3 ч" on cards, one for the whole app, moving once a minute: cards stopped asking the system
 * clock on every recomposition while a row scrolls in, and their text refreshes together when the minute turns.
 */
object MinuteClock {
    private val state = mutableLongStateOf(System.currentTimeMillis())

    /** Epoch millis, updated at most once a minute; reading it recomposes the reader when it moves. */
    val now: Long get() = state.longValue

    /** Keeps the clock moving while the app is composed; call once at the root. */
    @Composable
    fun Ticker() {
        LaunchedEffect(Unit) {
            while (true) {
                val current = System.currentTimeMillis()
                state.longValue = current
                delay(MinuteMillis - current % MinuteMillis)
            }
        }
    }

    private const val MinuteMillis = 60_000L
}
