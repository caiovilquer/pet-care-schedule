package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.FakeClock
import dev.vilquer.petcarescheduler.application.InMemoryEventRepo
import dev.vilquer.petcarescheduler.application.InMemoryPetRepo
import dev.vilquer.petcarescheduler.application.InMemoryTutorRepo
import dev.vilquer.petcarescheduler.application.exception.NotFoundException
import dev.vilquer.petcarescheduler.core.domain.entity.Event
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.Status
import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class DashboardAppServiceTest {
    private val tutorId = TutorId(1)
    private val now = LocalDateTime.of(2026, 7, 12, 9, 0)
    private val clock = FakeClock(ZonedDateTime.of(now, ZoneId.of("America/Sao_Paulo")))

    @Test
    fun `overview returns profile totals pets and only upcoming seven-day events`() {
        val tutors = InMemoryTutorRepo()
        tutors.save(
            Tutor(
                id = tutorId,
                firstName = "Ana",
                lastName = "Silva",
                email = Email.of("ana@example.com").getOrThrow(),
                passwordHash = "hash",
                phoneNumber = null,
                avatar = "https://example.com/ana.jpg",
            ),
        )
        val pets = InMemoryPetRepo()
        val luna = pets.save(
            Pet(name = "Luna", species = "cat", breed = null, birthdate = null, tutorId = tutorId),
        )
        pets.save(Pet(name = "Tobias", species = "dog", breed = null, birthdate = null, tutorId = tutorId))
        val events = InMemoryEventRepo(countsByTutor = mapOf(tutorId to 3L))
        events.save(
            Event(
                type = EventType.VACCINE,
                description = "Reforço",
                dateStart = now.plusDays(2),
                recurrence = null,
                status = Status.PENDING,
                petId = luna.id!!,
            ),
        )
        events.save(
            Event(
                type = EventType.SERVICE,
                description = "Banho",
                dateStart = now.plusDays(8),
                recurrence = null,
                status = Status.PENDING,
                petId = luna.id!!,
            ),
        )
        events.save(
            Event(
                type = EventType.MEDICINE,
                description = "Dose",
                dateStart = now.plusDays(1),
                recurrence = null,
                status = Status.DONE,
                petId = luna.id!!,
            ),
        )

        val result = DashboardAppService(tutors, pets, events, clock).getOverview(tutorId)

        assertEquals("Ana", result.firstName)
        assertEquals("ana@example.com", result.email)
        assertEquals(2, result.totalPets)
        assertEquals(3, result.totalEvents)
        assertEquals(listOf("Luna", "Tobias"), result.pets.map { it.name })
        assertEquals(1, result.upcomingEvents.size)
        assertEquals("Reforço", result.upcomingEvents.single().description)
    }

    @Test
    fun `overview rejects an unknown tutor`() {
        val service = DashboardAppService(
            InMemoryTutorRepo(),
            InMemoryPetRepo(),
            InMemoryEventRepo(),
            clock,
        )

        assertThrows(NotFoundException::class.java) { service.getOverview(tutorId) }
    }
}
