package dev.vilquer.petcarescheduler.core.domain.care

import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Frequency
import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class CarePlanTest {
    private val start = LocalDateTime.of(2026, 7, 12, 10, 0)
    private val generatedAt = Instant.parse("2026-07-12T12:00:00Z")

    @Test
    fun `occurrence ids are deterministic per plan revision and sequence`() {
        val plan = plan(Recurrence(Frequency.DAILY, repetitions = 2))
        val firstPass = plan.occurrencesThrough(start.plusDays(5), generatedAt)
        val secondPass = plan.occurrencesThrough(start.plusDays(5), generatedAt.plusSeconds(60))

        assertEquals(firstPass.map { it.id }, secondPass.map { it.id })
        assertNotEquals(firstPass.first().id, plan.copy(scheduleRevision = 1).occurrencesThrough(start.plusDays(5), generatedAt).first().id)
    }

    @Test
    fun `final date is inclusive and horizon generation is bounded`() {
        val inclusive = plan(Recurrence(Frequency.DAILY, finalDate = start.plusDays(2)))
        assertEquals(3, inclusive.occurrencesThrough(start.plusDays(10), generatedAt).size)

        val unbounded = plan(Recurrence(Frequency.DAILY))
        assertEquals(
            CarePlan.MAX_OCCURRENCES_PER_HORIZON,
            unbounded.occurrencesThrough(start.plusYears(10), generatedAt).size,
        )
    }

    private fun plan(recurrence: Recurrence?) = CarePlan(
        householdId = HouseholdId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
        tutorId = TutorId(1),
        petId = PetId(1),
        responsibleTutorId = TutorId(1),
        type = EventType.MEDICINE,
        title = "Remédio",
        startAt = start,
        recurrence = recurrence,
        createdAt = generatedAt,
        updatedAt = generatedAt,
    )
}
