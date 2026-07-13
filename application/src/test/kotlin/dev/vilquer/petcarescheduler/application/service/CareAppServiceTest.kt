package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.FakeCareReminderOutbox
import dev.vilquer.petcarescheduler.application.FakeClock
import dev.vilquer.petcarescheduler.application.FakeTransactionPort
import dev.vilquer.petcarescheduler.application.InMemoryCareOccurrenceActionRepo
import dev.vilquer.petcarescheduler.application.InMemoryCareOccurrenceRepo
import dev.vilquer.petcarescheduler.application.InMemoryCarePlanRepo
import dev.vilquer.petcarescheduler.application.InMemoryPetRepo
import dev.vilquer.petcarescheduler.application.InMemoryTutorRepo
import dev.vilquer.petcarescheduler.application.FakeCareEscalationOutbox
import dev.vilquer.petcarescheduler.application.FakeHouseholdMemberRepo
import dev.vilquer.petcarescheduler.application.FakeHouseholdActivityRepo
import dev.vilquer.petcarescheduler.application.TEST_HOUSEHOLD_ID
import dev.vilquer.petcarescheduler.application.exception.ConflictException
import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrenceId
import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrenceStatus
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdAccess
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdRole
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.core.domain.valueobject.Frequency
import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import dev.vilquer.petcarescheduler.usecase.command.CompleteCareOccurrenceCommand
import dev.vilquer.petcarescheduler.usecase.command.CreateCarePlanCommand
import dev.vilquer.petcarescheduler.usecase.command.UndoCareOccurrenceCommand
import dev.vilquer.petcarescheduler.usecase.command.UpdateCarePlanCommand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

class CareAppServiceTest {
    private val tutorId = TutorId(1)
    private val access = HouseholdAccess(TEST_HOUSEHOLD_ID, tutorId, HouseholdRole.OWNER)
    private val localNow = LocalDateTime.of(2026, 7, 12, 9, 0)
    private val clock = FakeClock(ZonedDateTime.of(localNow, ZoneId.of("America/Sao_Paulo")))
    private lateinit var plans: InMemoryCarePlanRepo
    private lateinit var occurrences: InMemoryCareOccurrenceRepo
    private lateinit var actions: InMemoryCareOccurrenceActionRepo
    private lateinit var outbox: FakeCareReminderOutbox
    private lateinit var pets: InMemoryPetRepo
    private lateinit var tutors: InMemoryTutorRepo
    private lateinit var service: CareAppService
    private var petId = dev.vilquer.petcarescheduler.core.domain.entity.PetId(0)

    @BeforeEach
    fun setUp() {
        plans = InMemoryCarePlanRepo()
        occurrences = InMemoryCareOccurrenceRepo()
        actions = InMemoryCareOccurrenceActionRepo()
        outbox = FakeCareReminderOutbox()
        tutors = InMemoryTutorRepo()
        tutors.save(
            Tutor(
                id = tutorId,
                firstName = "Ana",
                lastName = "Silva",
                email = Email.of("ana@example.com").getOrThrow(),
                passwordHash = "hash",
                phoneNumber = null,
            ),
        )
        pets = InMemoryPetRepo()
        petId = pets.save(Pet(name = "Luna", species = "cat", breed = null, birthdate = null, tutorId = tutorId, householdId = TEST_HOUSEHOLD_ID)).id!!
        service = CareAppService(plans, occurrences, actions, pets, tutors, outbox, FakeCareEscalationOutbox(),
            FakeHouseholdMemberRepo(tutorId), FakeHouseholdActivityRepo(), FakeTransactionPort(), clock)
    }

    @Test
    fun `recurring plan materializes independent occurrences without waiting for completion`() {
        service.create(dailyPlan(repetitions = 4), access)

        val generated = occurrences.all().sortedBy { it.dueAt }
        assertEquals(4, generated.size)
        assertEquals(listOf(0, 1, 2, 3), generated.map { it.sequence })
        assertTrue(generated.all { it.status == CareOccurrenceStatus.SCHEDULED })
    }

    @Test
    fun `editing a plan preserves completed history and replaces only future scheduled occurrences`() {
        val created = service.create(dailyPlan(repetitions = 3), access)
        val first = occurrences.all().minBy { it.dueAt }
        service.complete(CompleteCareOccurrenceCommand(first.id, UUID.randomUUID(), null), access)

        service.update(
            UpdateCarePlanCommand(
                planId = dev.vilquer.petcarescheduler.core.domain.care.CarePlanId(created.id),
                type = EventType.MEDICINE,
                title = "Nova dose",
                instructions = "Após a refeição",
                startAt = localNow.plusDays(2),
                recurrence = Recurrence(Frequency.DAILY, repetitions = 2),
                reminderMinutesBefore = 30,
            ),
            access,
        )

        val all = occurrences.all()
        assertEquals(CareOccurrenceStatus.COMPLETED, all.single { it.id == first.id }.status)
        assertEquals(2, all.count { it.status == CareOccurrenceStatus.CANCELLED })
        assertEquals(2, all.count { it.status == CareOccurrenceStatus.SCHEDULED && it.title == "Nova dose" })
        assertEquals(1, plans.all().single().scheduleRevision)
    }

    @Test
    fun `completion is replayable with the same request and rejects a second administration`() {
        service.create(dailyPlan(repetitions = 1), access)
        val occurrence = occurrences.all().single()
        val requestId = UUID.randomUUID()

        val first = service.complete(CompleteCareOccurrenceCommand(occurrence.id, requestId, "Administrado"), access)
        val replay = service.complete(CompleteCareOccurrenceCommand(occurrence.id, requestId, "Administrado"), access)

        assertEquals(first, replay)
        assertEquals(1, actions.all().size)
        assertThrows(ConflictException::class.java) {
            service.complete(CompleteCareOccurrenceCommand(occurrence.id, UUID.randomUUID(), null), access)
        }
    }

    @Test
    fun `undo is allowed for the same tutor inside the safety window and expires afterwards`() {
        service.create(dailyPlan(repetitions = 2), access)
        val ordered = occurrences.all().sortedBy { it.dueAt }
        service.complete(CompleteCareOccurrenceCommand(ordered[0].id, UUID.randomUUID(), null), access)
        service.complete(CompleteCareOccurrenceCommand(ordered[1].id, UUID.randomUUID(), null), access)

        clock.fixed = clock.fixed.plusMinutes(9)
        val reopened = service.undo(UndoCareOccurrenceCommand(ordered[0].id, UUID.randomUUID()), access)
        assertEquals(CareOccurrenceStatus.SCHEDULED, reopened.status)

        clock.fixed = clock.fixed.plusMinutes(2)
        assertThrows(ConflictException::class.java) {
            service.undo(UndoCareOccurrenceCommand(ordered[1].id, UUID.randomUUID()), access)
        }
    }

    @Test
    fun `maintenance extends the horizon idempotently and enqueues a due reminder once`() {
        service.create(
            dailyPlan(repetitions = 1).copy(
                startAt = localNow.plusMinutes(30),
                reminderMinutesBefore = 60,
            ),
            access,
        )

        service.materializeAndEnqueueReminders()
        service.materializeAndEnqueueReminders()

        assertEquals(1, occurrences.all().size)
        assertEquals(1, outbox.all().size)
    }

    private fun dailyPlan(repetitions: Int) = CreateCarePlanCommand(
        petId = petId,
        type = EventType.MEDICINE,
        title = "Antibiótico",
        instructions = "Dose prescrita",
        startAt = localNow.plusHours(1),
        recurrence = Recurrence(Frequency.DAILY, repetitions = repetitions),
        reminderMinutesBefore = 15,
    )
}
