package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.*
import dev.vilquer.petcarescheduler.core.domain.entity.*
import dev.vilquer.petcarescheduler.usecase.command.DeleteEventCommand
import dev.vilquer.petcarescheduler.usecase.command.RegisterEventCommand
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
    private val notifier = FakeNotifier()
    private val service = EventAppService(eventRepo, petRepo, clock, notifier)

    @Test
    fun `registerEvent should throw when pet does not exist`() {
        val cmd = RegisterEventCommand(PetId(1), EventType.VACCINE, "vac", LocalDateTime.now())
        assertThrows(IllegalArgumentException::class.java) { service.registerEvent(cmd) }
    }

    @Test
    fun `registerEvent persists event and notifies`() {
        val pet = petRepo.save(Pet(id = PetId(1), name="rex", specie="dog", race=null, birthdate= LocalDate.now(), tutorId = TutorId(1)))
        val cmd = RegisterEventCommand(pet.id!!, EventType.SERVICE, "bath", LocalDateTime.of(2025,7,2,9,0))

        val result: EventRegisteredResult = service.registerEvent(cmd)

        assertEquals(EventId(1), result.eventId)
        val saved = eventRepo.findById(result.eventId)
        assertEquals(Status.PENDING, saved?.status)
        assertEquals(1, notifier.notified.size)
        assertEquals(saved, notifier.notified.first())
    }

    @Test
    fun `deleteEvent marks event done`() {
        val pet = petRepo.save(Pet(id = PetId(1), name="rex", specie="dog", race=null, birthdate= LocalDate.now(), tutorId = TutorId(1)))
        val ev = eventRepo.save(Event(type=EventType.DIARY, description=null, dateStart=LocalDateTime.now(), recurrence=null, status=Status.PENDING, petId=pet.id!!))
        service.deleteEvent(DeleteEventCommand(ev.id!!))
        val updated = eventRepo.findById(ev.id!!)
        assertEquals(Status.DONE, updated?.status)
    }

    @Test
    fun `sendRemindersForToday notifies only today's pending events`() {
        val pet = petRepo.save(Pet(id = PetId(1), name="rex", specie="dog", race=null, birthdate= LocalDate.now(), tutorId = TutorId(1)))
        val today = clock.fixed.toLocalDate()
        eventRepo.save(Event(type=EventType.SERVICE, description=null, dateStart=today.atStartOfDay(), recurrence=null, status=Status.PENDING, petId=pet.id!!))
        eventRepo.save(Event(type=EventType.SERVICE, description=null, dateStart=today.minusDays(1).atStartOfDay(), recurrence=null, status=Status.PENDING, petId=pet.id!!))
        eventRepo.save(Event(type=EventType.SERVICE, description=null, dateStart=today.atStartOfDay(), recurrence=null, status=Status.DONE, petId=pet.id!!))

        service.sendRemindersForToday()

        assertEquals(1, notifier.notified.size)
        assertEquals(Status.PENDING, notifier.notified.first().status)
        assertEquals(today, notifier.notified.first().dateStart.toLocalDate())
    }
}
