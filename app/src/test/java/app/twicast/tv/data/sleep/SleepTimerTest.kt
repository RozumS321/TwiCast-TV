package app.twicast.tv.data.sleep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerTest {
    private val minute = SleepTimer.MinuteMillis

    @Test
    fun `a chosen time counts down and fires once`() {
        val timer = SleepTimer()
        timer.choose(SleepChoice.Minutes30, now = 1_000)
        assertEquals(30 * minute, timer.state.value.remainingMillis(1_000))
        assertFalse(timer.consumeIfDue(1_000 + 30 * minute - 1))
        assertTrue(timer.consumeIfDue(1_000 + 30 * minute))
        assertEquals(SleepChoice.Off, timer.state.value.choice)
        assertFalse(timer.consumeIfDue(1_000 + 31 * minute))
    }

    @Test
    fun `extend gives fifteen more minutes from now, cancel switches it off`() {
        val timer = SleepTimer()
        timer.choose(SleepChoice.Hour1, now = 0)
        timer.extend(now = 59 * minute)
        assertEquals(15 * minute, timer.state.value.remainingMillis(59 * minute))
        timer.cancel()
        assertFalse(timer.state.value.active)
        assertNull(timer.state.value.remainingMillis(0))
    }

    @Test
    fun `until the stream ends waits, then gives a last minute`() {
        val timer = SleepTimer()
        timer.choose(SleepChoice.StreamEnd, now = 0)
        assertNull(timer.state.value.remainingMillis(10 * minute))
        assertFalse(timer.consumeIfDue(10 * minute))
        timer.onStreamEnded(now = 10 * minute)
        assertEquals(SleepTimer.WarningMillis, timer.state.value.remainingMillis(10 * minute))
        assertTrue(timer.consumeIfDue(11 * minute))
    }

    @Test
    fun `a stream ending does not touch a timed choice`() {
        val timer = SleepTimer()
        timer.choose(SleepChoice.Minutes15, now = 0)
        timer.onStreamEnded(now = minute)
        assertEquals(14 * minute, timer.state.value.remainingMillis(minute))
    }
}
