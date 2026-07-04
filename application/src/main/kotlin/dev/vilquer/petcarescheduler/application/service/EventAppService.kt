package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.exception.ForbiddenException
import dev.vilquer.petcarescheduler.application.mapper.toDetailResult
import dev.vilquer.petcarescheduler.application.mapper.toSummary
import dev.vilquer.petcarescheduler.core.domain.entity.*
import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.*
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.*
import dev.vilquer.petcarescheduler.usecase.result.EventDetailResult
import dev.vilquer.petcarescheduler.usecase.result.EventRegisteredResult
import dev.vilquer.petcarescheduler.usecase.result.EventsPageResult

class EventAppService(
    private val eventRepo: EventRepositoryPort,
    private val petRepo: PetRepositoryPort,
    private val clock: ClockPort,
    private val notifier: NotificationPort
) :
    RegisterEventUseCase,
    DeleteEventUseCase,
    UpdateEventUseCase,
    ToggleEventUseCase,
    ListEventsUseCase,
    ListPetEventsUseCase,
    GetEventUseCase,
    SendDailyRemindersUseCase

{
    override fun execute(cmd: RegisterEventCommand, tutorId: TutorId): EventRegisteredResult {
        if (!petRepo.existsForTutor(cmd.petId, tutorId)) {
            throw ForbiddenException("Não pode registrar evento para pet de outro tutor")
        }
        val toSave = Event(
            type = cmd.type,
            description = cmd.description,
            dateStart = cmd.dateStart,
            recurrence = cmd.recurrence,
            status = Status.PENDING,
            petId = cmd.petId
        )
        val saved = eventRepo.save(toSave)
        // Notificação é responsabilidade do scheduler diário, não do cadastro:
        // disparar aqui enviava um "lembrete" de um evento que pode ocorrer daqui
        // a meses, no meio da requisição de criação.
        return EventRegisteredResult(saved.id!!)
    }

    override fun execute(cmd: DeleteEventCommand, tutorId: TutorId) {
        if (!eventRepo.existsForTutor(cmd.eventId, tutorId))
            throw ForbiddenException("Não pode deletar evento de outro tutor")
        eventRepo.delete(cmd.eventId)
    }

    override fun execute(cmd: ToggleEventCommand, tutorId: TutorId) {
        val event = eventRepo.findByIdAndTutor(cmd.eventId, tutorId)
            ?: throw IllegalArgumentException("Event ${cmd.eventId.value} not found")
        if (event.status == Status.PENDING) eventRepo.save(event.markDone())
        else eventRepo.save(event.markPending())
    }


    override fun execute(cmd: UpdateEventCommand, tutorId: TutorId): EventDetailResult {
        if (!eventRepo.existsForTutor(cmd.eventId, tutorId))
            throw ForbiddenException("Não pode alterar evento de outro tutor")
        val existing = eventRepo.findByIdAndTutor(cmd.eventId, tutorId)
            ?: throw IllegalArgumentException("Event ${cmd.eventId.value} not found")

        val updatedRecurrence =
            if (cmd.frequency != null || cmd.intervalCount != null || cmd.repetitions != null || cmd.finalDate != null) {
                val base = existing.recurrence ?: Recurrence()
                base.copy(
                    frequency = cmd.frequency ?: base.frequency,
                    intervalCount = cmd.intervalCount ?: base.intervalCount,
                    repetitions = cmd.repetitions ?: base.repetitions,
                    finalDate = cmd.finalDate ?: base.finalDate,
                )
            } else existing.recurrence
        val updated = existing.copy(
            type = cmd.type ?: existing.type,
            description = cmd.description ?: existing.description,
            recurrence = updatedRecurrence,
            dateStart = cmd.dateStart ?: existing.dateStart
        )
        val saved = eventRepo.save(updated)
        return saved.toDetailResult()
    }
    override fun list(tutorId: TutorId, page: Int, size: Int): EventsPageResult {
        val items = eventRepo.listByTutor(tutorId, page, size).map { it.toSummary() }
        val total = eventRepo.countByTutor(tutorId)
        return EventsPageResult(items, total, page, size)
    }

    override fun list(petId: PetId, tutorId: TutorId): List<EventDetailResult> {
        if (!petRepo.existsForTutor(petId, tutorId))
            throw ForbiddenException("Não pode listar eventos de pet de outro tutor")
        return eventRepo.findByPetId(petId).map { it.toDetailResult() }
    }

    override fun get(id: EventId, tutorId: TutorId): EventDetailResult =
        eventRepo.findByIdAndTutor(id, tutorId)?.toDetailResult()
            ?: throw IllegalArgumentException("Event ${id.value} not found")

    override fun sendRemindersForToday() {
        val now = clock.now()
        val start = now.toLocalDate().atStartOfDay()
        val end = start.plusDays(1)
        eventRepo.findPendingReminders(start, end)
            .forEach { notifier.sendEventReminder(it) }
    }
}
