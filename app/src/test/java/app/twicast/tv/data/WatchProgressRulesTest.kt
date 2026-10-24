package app.twicast.tv.data

import app.twicast.tv.data.repository.WatchProgress
import app.twicast.tv.data.repository.WatchProgressRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchProgressRulesTest {
    private fun at(positionMs: Long, durationMs: Long = 3_600_000, updated: Long = 0) = WatchProgress(positionMs, durationMs, updated)

    @Test
    fun `a peek under 30 s is not remembered, and wipes an older position`() {
        val all = WatchProgressRules.updated(mapOf("v" to at(600_000)), "v", at(10_000))
        assertTrue(all.isEmpty())
    }

    @Test
    fun `the last two minutes count as watched`() {
        val nearEnd = at(positionMs = 3_600_000 - 90_000)
        assertTrue(nearEnd.isFinished)
        assertNull("starts over", nearEnd.resumePositionMs)
        assertEquals(1f, nearEnd.watchedFraction)

        val middle = at(positionMs = 900_000)
        assertFalse(middle.isFinished)
        assertEquals(900_000L, middle.resumePositionMs)
        assertEquals(0.25f, middle.watchedFraction)
    }

    @Test
    fun `only the newest 100 recordings are kept`() {
        var all = emptyMap<String, WatchProgress>()
        for (i in 1..105) all = WatchProgressRules.updated(all, "v$i", at(60_000, updated = i.toLong()))
        assertEquals(WatchProgressRules.MaxRecordings, all.size)
        assertFalse("v1" in all)
        assertTrue("v105" in all)
    }
}
