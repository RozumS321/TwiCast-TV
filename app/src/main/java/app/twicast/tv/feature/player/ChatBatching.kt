package app.twicast.tv.feature.player

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.produceIn

/**
 * Groups items that arrive within [periodMillis] after the first one into a single list. A busy chat sends
 * dozens of lines a second; one update per batch keeps the panel from recomposing on every line.
 * Nothing is emitted (and nothing ticks) while no items come.
 */
internal fun <T : Any> Flow<T>.batchedBy(periodMillis: Long): Flow<List<T>> = channelFlow {
    val upstream = buffer(Channel.UNLIMITED).produceIn(this)
    while (true) {
        val first = upstream.receiveCatching()
        if (first.isClosed) {
            first.exceptionOrNull()?.let { throw it }
            break
        }
        val batch = mutableListOf(first.getOrThrow())
        delay(periodMillis)
        var next = upstream.tryReceive().getOrNull()
        while (next != null) {
            batch += next
            next = upstream.tryReceive().getOrNull()
        }
        send(batch)
    }
}
