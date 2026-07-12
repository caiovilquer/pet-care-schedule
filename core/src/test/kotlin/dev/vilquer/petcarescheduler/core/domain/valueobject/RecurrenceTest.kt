package dev.vilquer.petcarescheduler.core.domain.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class RecurrenceTest {

    private val start = LocalDateTime.of(2026, 1, 1, 10, 0)

    @Test
    fun `rejects intervalCount zero or negative`() {
        assertThrows(IllegalArgumentException::class.java) { Recurrence(intervalCount = 0) }
        assertThrows(IllegalArgumentException::class.java) { Recurrence(intervalCount = -1) }
    }

    @Test
    fun `rejects repetitions zero or negative when present`() {
        assertThrows(IllegalArgumentException::class.java) { Recurrence(repetitions = 0) }
        assertThrows(IllegalArgumentException::class.java) { Recurrence(repetitions = -1) }
    }

    @Test
    fun `nextOccurrence advances by the configured unit and interval`() {
        assertEquals(start.plusDays(1), Recurrence(Frequency.DAILY, 1).nextOccurrence(start))
        assertEquals(start.plusDays(3), Recurrence(Frequency.DAILY, 3).nextOccurrence(start))
        assertEquals(start.plusWeeks(1), Recurrence(Frequency.WEEKLY, 1).nextOccurrence(start))
        assertEquals(start.plusMonths(2), Recurrence(Frequency.MONTHLY, 2).nextOccurrence(start))
        assertEquals(start.plusYears(1), Recurrence(Frequency.YEARLY, 1).nextOccurrence(start))
    }

    @Test
    fun `hasNext is true with no repetitions or finalDate configured`() {
        val recurrence = Recurrence(Frequency.DAILY)
        assertTrue(recurrence.hasNext(executeCount = 1, lastDate = start))
        assertTrue(recurrence.hasNext(executeCount = 1000, lastDate = start))
    }

    @Test
    fun `hasNext becomes false once repetitions are exhausted`() {
        val recurrence = Recurrence(Frequency.DAILY, repetitions = 3)
        assertTrue(recurrence.hasNext(executeCount = 2, lastDate = start))
        assertFalse(recurrence.hasNext(executeCount = 3, lastDate = start))
        assertFalse(recurrence.hasNext(executeCount = 4, lastDate = start))
    }

    @Test
    fun `hasNext includes finalDate and becomes false after it`() {
        val finalDate = start.plusDays(10)
        val recurrence = Recurrence(Frequency.DAILY, finalDate = finalDate)
        assertTrue(recurrence.hasNext(executeCount = 1, lastDate = finalDate.minusDays(1)))
        assertTrue(recurrence.hasNext(executeCount = 1, lastDate = finalDate))
        assertFalse(recurrence.hasNext(executeCount = 1, lastDate = finalDate.plusDays(1)))
    }
}
