package app.twicast.tv.data

import app.twicast.tv.data.repository.newlyLive
import app.twicast.tv.testing.stream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewlyLiveTest {
    @Test
    fun `the first check only notes who is live`() {
        assertTrue(newlyLive(previous = null, current = listOf(stream("a"), stream("b"))).isEmpty())
    }

    @Test
    fun `only channels that were not live at the last check are announced`() {
        val announced = newlyLive(previous = setOf("login_a"), current = listOf(stream("a"), stream("b")))
        assertEquals(listOf("login_b"), announced.map { it.userLogin })
    }

    @Test
    fun `logins compare without case, and a channel back on air after going off is announced again`() {
        assertTrue(newlyLive(previous = setOf("LOGIN_A".lowercase()), current = listOf(stream("a"))).isEmpty())
        // Was live, went off (not in the last set), live again: announced.
        assertEquals(1, newlyLive(previous = emptySet(), current = listOf(stream("a"))).size)
    }
}
