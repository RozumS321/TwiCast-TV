package app.twicast.tv.feature.channel

import app.twicast.tv.data.remote.dto.ScheduleCategoryDto
import app.twicast.tv.data.remote.dto.ScheduleDto
import app.twicast.tv.data.remote.dto.ScheduleSegmentDto
import app.twicast.tv.data.remote.dto.ScheduleVacationDto
import app.twicast.tv.data.repository.upcomingStreams
import app.twicast.tv.feature.channel.components.ScheduleDay
import app.twicast.tv.feature.channel.components.scheduleDay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ScheduleTest {
    private val now = Instant.parse("2026-10-24T12:00:00Z").toEpochMilli()

    private fun segment(start: String, end: String? = null, title: String = "", canceled: Boolean = false) = ScheduleSegmentDto(
        startTime = start,
        endTime = end,
        title = title,
        canceledUntil = if (canceled) "2026-11-01T00:00:00Z" else null,
        category = ScheduleCategoryDto("Counter-Strike"),
    )

    @Test
    fun `upcoming streams skip the past and the cancelled, soonest first`() {
        val schedule = ScheduleDto(
            segments = listOf(
                segment("2026-10-26T15:00:00Z", title = "later"),
                segment("2026-10-23T15:00:00Z", "2026-10-23T19:00:00Z", title = "past"),
                segment("2026-10-25T15:00:00Z", "2026-10-25T19:00:00Z", title = "next"),
                segment("2026-10-25T20:00:00Z", title = "cancelled", canceled = true),
            ),
        )
        assertEquals(listOf("next", "later"), upcomingStreams(schedule, now).map { it.title })
        assertEquals("Counter-Strike", upcomingStreams(schedule, now).first().categoryName)
    }

    @Test
    fun `a stream on air now still counts until it ends`() {
        val schedule = ScheduleDto(segments = listOf(segment("2026-10-24T11:00:00Z", "2026-10-24T14:00:00Z", title = "now")))
        assertEquals(listOf("now"), upcomingStreams(schedule, now).map { it.title })
    }

    @Test
    fun `streams in the vacation are dropped, at most five are kept`() {
        val segments = (25..31).map { segment("2026-10-${it}T15:00:00Z", title = "d$it") }
        val vacation = ScheduleVacationDto("2026-10-25T00:00:00Z", "2026-10-26T00:00:00Z")
        val upcoming = upcomingStreams(ScheduleDto(segments, vacation), now)
        assertEquals(listOf("d26", "d27", "d28", "d29", "d30"), upcoming.map { it.title })
    }

    @Test
    fun `days read as today, tomorrow or a date in the viewer's time zone`() {
        val kyiv = ZoneId.of("Europe/Kyiv")
        assertEquals(ScheduleDay.Today, scheduleDay(Instant.parse("2026-10-24T18:00:00Z").toEpochMilli(), now, kyiv))
        // 23:30 UTC is already the next day in Kyiv.
        assertEquals(ScheduleDay.Tomorrow, scheduleDay(Instant.parse("2026-10-24T23:30:00Z").toEpochMilli(), now, kyiv))
        assertEquals(ScheduleDay.Later, scheduleDay(Instant.parse("2026-10-27T15:00:00Z").toEpochMilli(), now, kyiv))
    }
}
