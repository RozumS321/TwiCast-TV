package app.twicast.tv.feature.player

import android.os.SystemClock
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twicast.tv.data.sleep.DeviceSleeper
import app.twicast.tv.data.sleep.SleepChoice
import app.twicast.tv.data.sleep.SleepTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Which part of the sleep timer is on screen over the player. */
enum class SleepPanel { None, Menu, Ask }

/**
 * @param leftMillis time to sleep, null when off or waiting for the stream to end.
 * @param warning the last minute: the "засыпаем через минуту" dialog is up.
 */
@Immutable
data class SleepUiState(
    val panel: SleepPanel = SleepPanel.None,
    val choice: SleepChoice = SleepChoice.Off,
    val leftMillis: Long? = null,
    val warning: Boolean = false,
    val allowed: Boolean = false,
) {
    /** Any sleep UI takes the remote: the player's own controls stay down meanwhile. */
    val capturesKeys: Boolean get() = panel != SleepPanel.None || warning
}

@HiltViewModel
class SleepTimerViewModel @Inject constructor(
    private val timer: SleepTimer,
    private val sleeper: DeviceSleeper,
) : ViewModel() {
    /** Elapsed-realtime clock; tests replace it. */
    internal var clock: () -> Long = SystemClock::elapsedRealtime

    private val _ui = MutableStateFlow(SleepUiState())
    val ui: StateFlow<SleepUiState> = _ui.asStateFlow()

    private val _sleepNow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Time is up: leave the player and put the box to sleep. */
    val sleepNow: SharedFlow<Unit> = _sleepNow.asSharedFlow()

    init {
        viewModelScope.launch {
            while (true) {
                tick()
                delay(TickMillis)
            }
        }
    }

    fun onOpenMenu() = update { it.copy(panel = SleepPanel.Menu, allowed = sleeper.allowed) }

    fun onChoose(choice: SleepChoice) {
        timer.choose(choice, clock())
        val explain = choice != SleepChoice.Off && !sleeper.allowed && !sleeper.explained
        update { it.copy(panel = if (explain) SleepPanel.Ask else SleepPanel.None) }
        tick()
    }

    /** The viewer went to Android's own screen (or declined): the card is not shown again. */
    fun onExplained() {
        sleeper.explained = true
        update { it.copy(panel = SleepPanel.None) }
    }

    fun onExtend() {
        timer.extend(clock())
        tick()
    }

    fun onCancelTimer() {
        timer.cancel()
        tick()
    }

    /** Back closes whatever sleep UI is up; false when there was none. */
    fun onBack(): Boolean {
        val current = _ui.value
        return when {
            current.panel != SleepPanel.None -> {
                update { it.copy(panel = SleepPanel.None) }
                true
            }
            else -> false
        }
    }

    fun onStreamEnded() {
        timer.onStreamEnded(clock())
        tick()
    }

    /** Called back from Android's admin screen (or when the player resumes): the menu shows the real state. */
    fun onResume() = update { it.copy(allowed = sleeper.allowed) }

    /** Leaves the player first, then sleeps: true when the box was put to sleep. */
    fun sleepDevice(): Boolean = sleeper.sleepNow()

    fun adminRequest() = sleeper.requestIntent()

    internal fun tick() {
        val now = clock()
        if (timer.consumeIfDue(now)) {
            update { it.copy(choice = SleepChoice.Off, leftMillis = null, warning = false, panel = SleepPanel.None) }
            _sleepNow.tryEmit(Unit)
            return
        }
        val state = timer.state.value
        val left = state.remainingMillis(now)
        update { it.copy(choice = state.choice, leftMillis = left, warning = left != null && left <= SleepTimer.WarningMillis) }
    }

    private inline fun update(change: (SleepUiState) -> SleepUiState) {
        _ui.value = change(_ui.value)
    }

    private companion object {
        const val TickMillis = 1_000L
    }
}
