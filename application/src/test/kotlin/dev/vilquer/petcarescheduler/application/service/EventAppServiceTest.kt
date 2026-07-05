package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.*
import dev.vilquer.petcarescheduler.application.exception.ForbiddenException
import dev.vilquer.petcarescheduler.application.exception.NotFoundException
import dev.vilquer.petcarescheduler.core.domain.entity.*
import dev.vilquer.petcarescheduler.core.domain.valueobject.Frequency
import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import dev.vilquer.petcarescheduler.usecase.command.DeleteEventCommand
import dev.vilquer.petcarescheduler.usecase.command.RegisterEventCommand
import dev.vilquer.petcarescheduler.usecase.command.ToggleEventCommand
import dev.vilquer.petcarescheduler.usecase.result.EventRegisteredResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class EventAppServiceTest {

    private val petRepo = InMemoryPetRepo()
    private val eventRepo = InMemoryEventRepo()
    private val clock = FakeClock(ZonedDateTime.of(LocalDateTime.of(2025,7,1,8,0), ZoneId.systemDefault()))
    private val outbox = FakeReminderOutboxPort()
    private val service = EventAppService(eventRepo, petRepo, clock, outbox)
    private val tutorId = TutorId(1)

    @Test
    fun `registerEvent should throw when pet does not exist`() {
        val cmd = RegisterEventCommand(PetId(1), EventType.VACCINE, "vac", LocalDateTime.now())
        assertThrows(ForbiddenException::class.java) { service.execute(cmd, tutorId) }
    }

    @Test
    fun `registerEvent persists event without notifying immediately`() {
        val pet = petRepo.save(Pet(id = PetId(1), name="rex", species="dog", breed=null, birthdate= LocalDate.now(), tutorId = TutorId(1)))
        val cmd = RegisterEventCommand(pet.id!!, EventType.SERVICE, "bath", LocalDateTime.of(2025,7,2,9,0))

        val result: EventRegisteredResult = service.execute(cmd, tutorId)

        assertEquals(EventId(1), result.eventId)
        val saved = eventRepo.findById(result.eventId)
        assertEquals(Status.PENDING, saved?.status)
        // notificação é responsabilidade exclusiva do scheduler diário
        assertEquals(0, outbox.allMessages().size)
    }

    @Test
    fun `getEvent throws NotFoundException when event does not exist`() {
        assertThrows(NotFoundException::class.java) { service.get(EventId(999), tutorId) }
    }

    @Test
    fun `toggleEvent throws NotFoundException when event does not exist`() {
        assertThrows(NotFoundException::class.java) {
            service.execute(ToggleEventCommand(EventId(999)), tutorId)
        }
    }

    @Test
    fun `toggleEvent generates the next occurrence for a recurring event`() {
        val pet = petRepo.save(Pet(id = PetId(1), name="rex", species="dog", breed=null, birthdate= LocalDate.now(), tutorId = TutorId(1)))
        val dateStart = LocalDateTime.of(2025, 7, 1, 10, 0)
        val ev = eventRepo.save(
            Event(
                type = EventType.VACCINE,
                description = "shot",
                dateStart = dateStart,
                recurrence = Recurrence(Frequency.MONTHLY, intervalCount = 1),
                status = Status.PENDING,
                petId = pet.id!!
            )
        )

        service.execute(ToggleEventCommand(ev.id!!), tutorId)

        val completed = eventRepo.findById(ev.id!!)
        assertEquals(Status.DONE, completed?.status)

        val nextOccurrence = eventRepo.allEvents().singleOrNull { it.id != ev.id }
        assertNotNull(nextOccurrence, "a next occurrence should have been created")
        assertEquals(Status.PENDING, nextOccurrence!!.status)
        assertEquals(dateStart.plusMonths(1), nextOccurrence.dateStart)
        assertEquals(1, nextOccurrence.occurrenceCount)
    }

    @Test
    fun `toggleEvent creates no next occurrence without recurrence`() {
        val pet = petRepo.save(Pet(id = PetId(1), name="rex", species="dog", breed=null, birthdate= LocalDate.now(), tutorId = TutorId(1)))
        val ev = eventRepo.save(Event(type=EventType.DIARY, description=null, dateStart=LocalDateTime.now(), recurrence=null, status=Status.PENDING, petId=pet.id!!))

        service.execute(ToggleEventCommand(ev.id!!), tutorId)

        assertEquals(1, eventRepo.allEvents().size)
    }

    @Test
    fun `toggleEvent undo does not remove the already-created next occurrence`() {
        val pet = petRepo.save(Pet(id = PetId(1), name="rex", species="dog", breed=null, birthdate= LocalDate.now(), tutorId = TutorId(1)))
        val ev = eventRepo.save(
            Event(
                type = EventType.VACCINE,
                description = "shot",
                dateStart = LocalDateTime.of(2025, 7, 1, 10, 0),
                recurrence = Recurrence(Frequency.MONTHLY, intervalCount = 1),
                status = Status.PENDING,
                petId = pet.id!!
            )
        )

        service.execute(ToggleEventCommand(ev.id!!), tutorId) // PENDING -> DONE (creates next)
        service.execute(ToggleEventCommand(ev.id!!), tutorId) // DONE -> PENDING (undo)

        assertEquals(Status.PENDING, eventRepo.findById(ev.id!!)?.status)
        assertEquals(2, eventRepo.allEvents().size, "the next occurrence created earlier must remain")
    }

    @Test
    fun `deleteEvent removes event`() {
        val pet = petRepo.save(Pet(id = PetId(1), name="rex", species="dog", breed=null, birthdate= LocalDate.now(), tutorId = TutorId(1)))
        val ev = eventRepo.save(Event(type=EventType.DIARY, description=null, dateStart=LocalDateTime.now(), recurrence=null, status=Status.PENDING, petId=pet.id!!))
        service.execute(DeleteEventCommand(ev.id!!), tutorId)
        assertNull(eventRepo.findById(ev.id!!))
    }

    @Test
    fun `sendRemindersForToday enqueues only today's pending events`() {
        val pet = petRepo.save(Pet(id = PetId(1), name="rex", species="dog", breed=null, birthdate= LocalDate.now(), tutorId = TutorId(1)))
        val today = clock.fixed.toLocalDate()
        val todayPending = eventRepo.save(Event(type=EventType.SERVICE, description=null, dateStart=today.atStartOfDay(), recurrence=null, status=Status.PENDING, petId=pet.id!!))
        eventRepo.save(Event(type=EventType.SERVICE, description=null, dateStart=today.minusDays(1).atStartOfDay(), recurrence=null, status=Status.PENDING, petId=pet.id!!))
        eventRepo.save(Event(type=EventType.SERVICE, description=null, dateStart=today.atStartOfDay(), recurrence=null, status=Status.DONE, petId=pet.id!!))

        service.sendRemindersForToday()

        assertEquals(1, outbox.allMessages().size)
        assertEquals(todayPending.id, outbox.allMessages().first().eventId)
    }

    @Test
    fun `sendRemindersForToday does not enqueue the same event twice`() {
        val pet = petRepo.save(Pet(id = PetId(1), name="rex", species="dog", breed=null, birthdate= LocalDate.now(), tutorId = TutorId(1)))
        val today = clock.fixed.toLocalDate()
        eventRepo.save(Event(type=EventType.SERVICE, description=null, dateStart=today.atStartOfDay(), recurrence=null, status=Status.PENDING, petId=pet.id!!))

        service.sendRemindersForToday()
        service.sendRemindersForToday() // varredura roda de novo no mesmo dia

        assertEquals(1, outbox.allMessages().size, "enqueueIfAbsent deve ser idempotente")
    }
}
