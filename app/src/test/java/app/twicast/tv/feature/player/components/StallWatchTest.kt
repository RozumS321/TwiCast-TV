package app.twicast.tv.feature.player.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StallWatchTest {
    @Test
    fun `a moving picture is never stuck`() {
        val watch = StallWatch(10_000)
        for (t in 0..60) assertFalse(watch.isStuck(wantsToPlay = true, position = t * 2_000L, now = t * 2_000L))
    }

    @Test
    fun `a still picture that should play is stuck after the limit, then counts afresh`() {
        val watch = StallWatch(10_000)
        assertFalse(watch.isStuck(true, 5_000, 0))
        assertFalse(watch.isStuck(true, 5_000, 8_000))
        assertTrue(watch.isStuck(true, 5_000, 10_000))
        assertFalse(watch.isStuck(true, 5_000, 12_000))
        assertTrue(watch.isStuck(true, 5_000, 20_000))
    }

    @Test
    fun `a pause the viewer made is not a stall`() {
        val watch = StallWatch(10_000)
        for (t in 0..30) assertFalse(watch.isStuck(wantsToPlay = false, position = 5_000, now = t * 2_000L))
    }
}
