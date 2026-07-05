package dev.vilquer.petcarescheduler.core.domain.entity

import dev.vilquer.petcarescheduler.core.domain.valueobject.Frequency
import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class EventTest {

    private val start = LocalDateTime.of(2026, 1, 1, 9, 0)

    private fun event(recurrence: Recurrence? = null, occurrenceCount: Int = 0) = Event(
        id = EventId(1),
        type = EventType.VACCINE,
        description = "annual shot",
        dateStart = start,
        recurrence = recurrence,
        status = Status.PENDING,
        occurrenceCount = occurrenceCount,
        petId = PetId(1)
    )

    @Test
    fun `complete marks the event DONE and produces no next occurrence without recurrence`() {
        val (completed, next) = event(recurrence = null).complete()

        assertEquals(Status.DONE, completed.status)
        assertNull(next)
    }

    @Test
    fun `complete generates the next occurrence when recurrence is open-ended`() {
        val recurrence = Recurrence(Frequency.MONTHLY, intervalCount = 1)
        val (completed, next) = event(recurrence).complete()

        assertEquals(Status.DONE, completed.status)
        assertNotNull(next)
        assertEquals(Status.PENDING, next!!.status)
        assertNull(next.id)
        assertEquals(start.plusMonths(1), next.dateStart)
        assertEquals(1, next.occurrenceCount)
        assertEquals(event().petId, next.petId)
        assertEquals(event().type, next.type)
    }

    @Test
    fun `complete stops generating occurrences once repetitions are exhausted`() {
        val recurrence = Recurrence(Frequency.DAILY, repetitions = 2)
        val (_, next) = event(recurrence, occurrenceCount = 1).complete()

        assertNull(next, "second occurrence should be the last one for repetitions = 2")
    }

    @Test
    fun `complete keeps generating occurrences while repetitions remain`() {
        val recurrence = Recurrence(Frequency.DAILY, repetitions = 3)
        val (_, next) = event(recurrence, occurrenceCount = 1).complete()

        assertNotNull(next)
        assertEquals(2, next!!.occurrenceCount)
    }

    @Test
    fun `complete stops generating occurrences once finalDate has passed`() {
        val recurrence = Recurrence(Frequency.DAILY, finalDate = start.plusDays(1))
        val (_, next) = event(recurrence).complete()

        assertNull(next, "next occurrence would fall exactly on finalDate")
    }
}
