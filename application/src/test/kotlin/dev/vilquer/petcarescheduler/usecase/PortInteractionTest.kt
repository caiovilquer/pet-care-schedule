import dev.vilquer.petcarescheduler.core.domain.entity.*
import dev.vilquer.petcarescheduler.usecase.command.RegisterEventCommand
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.EventRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.EventReminderTarget
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.NotificationPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.RegisterEventUseCase
import dev.vilquer.petcarescheduler.usecase.result.EventRegisteredResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PortInteractionTest {

    private class FakeEventRepository : EventRepositoryPort {
        var received: Event? = null
        override fun save(event: Event): Event {
            received = event.copy(id = EventId(1L))
            return received!!
        }
        override fun findById(id: EventId): Event? = if (received?.id == id) received else null
        override fun findByPetId(petId: PetId): List<Event> = received?.takeIf { it.petId == petId }?.let { listOf(it) } ?: emptyList()
        override fun delete(id: EventId) {}
        override fun listByTutor(tutorId: TutorId, page: Int, size: Int): List<Event> = emptyList()
        override fun countByTutor(tutorId: TutorId): Long = 0
        override fun findUpcomingByTutor(
            tutorId: TutorId,
            start: LocalDateTime,
            end: LocalDateTime,
            limit: Int,
        ): List<Event> = emptyList()
        override fun findByIdAndTutor(id: EventId, tutorId: TutorId): Event? = findById(id)
        override fun existsForTutor(id: EventId, tutorId: TutorId): Boolean = received?.id == id
        override fun findPendingReminders(start: LocalDateTime, end: LocalDateTime): List<EventReminderTarget> =
            emptyList()
    }

    private class FakeNotification : NotificationPort {
        var notified: Event? = null
        override fun sendEventReminder(target: EventReminderTarget): Boolean {
            notified = target.event
            return true
        }
    }

    private class RegisterEventUseCaseService(
        private val repo: EventRepositoryPort,
        private val notifier: NotificationPort
    ) : RegisterEventUseCase {
        override fun execute(cmd: RegisterEventCommand, tutorId: TutorId): EventRegisteredResult {
            val toSave = Event(
                type = cmd.type,
                description = cmd.description,
                dateStart = cmd.dateStart,
                recurrence = null,
                status = Status.PENDING,
                petId = cmd.petId
            )
            val saved = repo.save(toSave)
            notifier.sendEventReminder(EventReminderTarget(saved, "tutor@example.com", null))
            return EventRegisteredResult(saved.id!!)
        }
    }

    @Test
    fun `execute should persist event and trigger notification`() {
        val repo = FakeEventRepository()
        val notifier = FakeNotification()
        val useCase = RegisterEventUseCaseService(repo, notifier)

        val cmd = RegisterEventCommand(
            petId = PetId(2L),
            type = EventType.SERVICE,
            description = "grooming",
            dateStart = LocalDateTime.of(2025, 7, 14, 9, 0)
        )

        val result = useCase.execute(cmd, TutorId(1L))

        assertEquals(EventId(1L), result.eventId)
        assertEquals(cmd.type, repo.received?.type)
        assertEquals(repo.received, notifier.notified)
    }
}
