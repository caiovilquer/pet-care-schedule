package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.exception.ForbiddenException
import dev.vilquer.petcarescheduler.application.exception.ConflictException
import dev.vilquer.petcarescheduler.application.exception.NotFoundException
import dev.vilquer.petcarescheduler.application.mapper.toDetailResult
import dev.vilquer.petcarescheduler.application.mapper.toSummary
import dev.vilquer.petcarescheduler.core.domain.entity.*
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.*
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.*
import dev.vilquer.petcarescheduler.usecase.result.EventDetailResult
import dev.vilquer.petcarescheduler.usecase.result.EventRegisteredResult
import dev.vilquer.petcarescheduler.usecase.result.EventsPageResult
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdTimezone

class EventAppService(
    private val eventRepo: EventRepositoryPort,
    private val petRepo: PetRepositoryPort,
    private val clock: ClockPort,
    private val outbox: ReminderOutboxPort,
    private val transaction: TransactionPort,
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
        transaction.execute {
            val event = eventRepo.findByIdAndTutor(cmd.eventId, tutorId)
                ?: throw NotFoundException("Event ${cmd.eventId.value} not found")
            if (event.status == Status.PENDING) {
                val (completed, next) = event.complete()
                eventRepo.save(completed)
                next?.let { eventRepo.save(it) }
            } else {
                if (event.recurrence != null) {
                    throw ConflictException(
                        "Cuidados recorrentes concluídos não podem ser reabertos porque a próxima ocorrência já foi criada"
                    )
                }
                eventRepo.save(event.markPending())
            }
        }
    }


    override fun execute(cmd: UpdateEventCommand, tutorId: TutorId): EventDetailResult {
        if (!eventRepo.existsForTutor(cmd.eventId, tutorId))
            throw ForbiddenException("Não pode alterar evento de outro tutor")
        val existing = eventRepo.findByIdAndTutor(cmd.eventId, tutorId)
            ?: throw NotFoundException("Event ${cmd.eventId.value} not found")

        val updated = existing.copy(
            type = cmd.type,
            description = cmd.description.trim(),
            recurrence = cmd.recurrence,
            dateStart = cmd.dateStart
        )
        return transaction.execute {
            if (existing.dateStart != updated.dateStart) {
                outbox.resetForEvent(cmd.eventId)
            }
            eventRepo.save(updated).toDetailResult()
        }
    }
    override fun list(tutorId: TutorId, page: Int, size: Int): EventsPageResult {
        require(page >= 0) { "page deve ser maior ou igual a zero" }
        require(size in 1..100) { "size deve estar entre 1 e 100" }
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
            ?: throw NotFoundException("Event ${id.value} not found")

    override fun sendRemindersForToday() {
        val now = clock.now()
        val scanStart = now.toLocalDate().atStartOfDay().minusDays(1)
        val scanEnd = scanStart.plusDays(3)
        // Só enfileira: a entrega de fato (com retry) é responsabilidade do
        // ReminderRelayService, rodando em outro scheduler. Isso evita que
        // uma API de e-mail lenta trave a varredura diária inteira.
        eventRepo.findPendingReminders(scanStart, scanEnd).forEach { target ->
            val localDate = clock.now(HouseholdTimezone.parse(target.timezone)).toLocalDate()
            if (target.event.dateStart.toLocalDate() != localDate) return@forEach
            outbox.enqueueIfAbsent(
                ReminderOutboxMessage(
                    eventId = target.event.id!!,
                    tutorEmail = target.tutorEmail,
                    petName = target.petName,
                    createdAt = now.toInstant(),
                    timezone = target.timezone,
                )
            )
        }
    }
}
