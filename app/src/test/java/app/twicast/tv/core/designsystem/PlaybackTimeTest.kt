package app.twicast.tv.core.designsystem

import app.twicast.tv.core.designsystem.format.formatPlaybackTime
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackTimeTest {
    @Test
    fun `hours show as h mm ss, shorter as m ss`() {
        assertEquals("1:24:10", formatPlaybackTime(5_050_000))
        assertEquals("7:42:10", formatPlaybackTime(27_730_000))
        assertEquals("4:05", formatPlaybackTime(245_000))
        assertEquals("0:00", formatPlaybackTime(-1))
    }
}
