package app.twicast.tv.feature.player

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatBatchingTest {

    @Test
    fun `lines within the period come as one batch, a pause starts a new one`() = runTest {
        val lines = flow {
            emit("a")
            delay(100)
            emit("b")
            delay(100)
            emit("c")
            delay(1_000)
            emit("d")
        }

        assertEquals(listOf(listOf("a", "b", "c"), listOf("d")), lines.batchedBy(250).toList())
    }

    @Test
    fun `an upstream error reaches the collector`() = runTest {
        val failing = flow<String> { throw IllegalStateException("dropped") }

        val error = runCatching { failing.batchedBy(250).toList() }.exceptionOrNull()

        assertEquals("dropped", error?.message)
    }
}
