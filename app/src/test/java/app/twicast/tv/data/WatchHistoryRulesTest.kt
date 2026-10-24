package app.twicast.tv.data

import app.twicast.tv.data.repository.HistoryEntry
import app.twicast.tv.data.repository.WatchHistoryRules
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchHistoryRulesTest {
    private fun live(login: String) = HistoryEntry(login, login)

    @Test
    fun `watching again moves the entry to the front instead of repeating it`() {
        val all = listOf(live("a"), live("b"), live("c"))
        assertEquals(listOf("live:b", "live:a", "live:c"), WatchHistoryRules.updated(all, live("B")).map { it.key })
    }

    @Test
    fun `a recording and its channel's live stream are separate cards`() {
        val all = WatchHistoryRules.updated(listOf(live("a")), HistoryEntry("a", "a", videoId = "v1"))
        assertEquals(listOf("v1", "live:a"), all.map { it.key })
    }

    @Test
    fun `only the newest 20 are kept`() {
        var all = emptyList<HistoryEntry>()
        for (i in 1..25) all = WatchHistoryRules.updated(all, live("c$i"))
        assertEquals(WatchHistoryRules.MaxEntries, all.size)
        assertEquals("live:c25", all.first().key)
    }
}
