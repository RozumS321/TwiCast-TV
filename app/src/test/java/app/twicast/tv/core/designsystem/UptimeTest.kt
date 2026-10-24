package app.twicast.tv.core.designsystem

import app.twicast.tv.core.designsystem.format.uptimeParts
import org.junit.Assert.assertEquals
import org.junit.Test

class UptimeTest {
    private val minute = 60_000L

    @Test
    fun `uptime splits into hours and minutes`() {
        assertEquals(2L to 14L, uptimeParts(startedAtMillis = 0, nowMillis = 134 * minute))
        assertEquals(0L to 35L, uptimeParts(startedAtMillis = 0, nowMillis = 35 * minute))
    }

    @Test
    fun `a start in the future counts as just started`() {
        assertEquals(0L to 0L, uptimeParts(startedAtMillis = 10 * minute, nowMillis = 0))
    }
}
