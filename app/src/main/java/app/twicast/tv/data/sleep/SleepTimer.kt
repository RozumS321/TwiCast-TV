package app.twicast.tv.data.sleep

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** What the sleep timer waits for (canvas "Плеер · таймер сна"). */
enum class SleepChoice(val minutes: Int?) {
    Off(null),
    Minutes15(15),
    Minutes30(30),
    Hour1(60),
    Hours2(120),

    /** Until the broadcast ends, then one more minute to say goodbye. */
    StreamEnd(null),
}

/** [endsAtMillis]: when it fires (elapsed-realtime clock); null while off, or waiting for the stream to end. */
@Immutable
data class SleepTimerState(val choice: SleepChoice = SleepChoice.Off, val endsAtMillis: Long? = null) {
    val active: Boolean get() = choice != SleepChoice.Off

    fun remainingMillis(now: Long): Long? = endsAtMillis?.let { (it - now).coerceAtLeast(0) }
}

/** Lives for the whole app: leaving the player does not cancel a timer, the next stream keeps counting down. */
@Singleton
class SleepTimer @Inject constructor() {
    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    fun choose(choice: SleepChoice, now: Long) {
        _state.value = SleepTimerState(choice, choice.minutes?.let { now + it * MinuteMillis })
    }

    /** «Ещё 15 минут» from the last-minute warning. */
    fun extend(now: Long) = choose(SleepChoice.Minutes15, now)

    fun cancel() {
        _state.value = SleepTimerState()
    }

    /** The broadcast ended: a "until it ends" timer starts its last minute. */
    fun onStreamEnded(now: Long) {
        val current = _state.value
        if (current.choice == SleepChoice.StreamEnd && current.endsAtMillis == null) {
            _state.value = current.copy(endsAtMillis = now + WarningMillis)
        }
    }

    /** True once when the time is up; the timer switches itself off. */
    fun consumeIfDue(now: Long): Boolean {
        val due = _state.value.remainingMillis(now) == 0L
        if (due) cancel()
        return due
    }

    companion object {
        const val MinuteMillis = 60_000L

        /** The warning shows for the last minute. */
        const val WarningMillis = MinuteMillis
    }
}
