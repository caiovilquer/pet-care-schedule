package dev.vilquer.petcarescheduler.application.adapter.input.rest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId

class CareWallClockTest {
    @Test
    fun `naive input remains household wall clock`() {
        assertEquals(
            LocalDateTime.of(2026, 7, 13, 9, 0),
            CareWallClock.parse("2026-07-13T09:00:00", ZoneId.of("America/New_York")),
        )
    }

    @Test
    fun `offset input is converted to household wall clock with DST`() {
        assertEquals(
            LocalDateTime.of(2026, 7, 13, 9, 0),
            CareWallClock.parse("2026-07-13T13:00:00Z", ZoneId.of("America/New_York")),
        )
        assertEquals(
            LocalDateTime.of(2026, 1, 13, 9, 0),
            CareWallClock.parse("2026-01-13T14:00:00Z", ZoneId.of("America/New_York")),
        )
    }
}
