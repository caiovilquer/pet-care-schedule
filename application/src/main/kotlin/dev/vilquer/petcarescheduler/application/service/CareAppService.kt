package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.exception.ConflictException
import dev.vilquer.petcarescheduler.application.exception.NotFoundException
import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrence
import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrenceAction
import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrenceActionType
import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrenceStatus
import dev.vilquer.petcarescheduler.core.domain.care.CarePlan
import dev.vilquer.petcarescheduler.core.domain.care.CarePlanId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.command.CompleteCareOccurrenceCommand
import dev.vilquer.petcarescheduler.usecase.command.CreateCarePlanCommand
import dev.vilquer.petcarescheduler.usecase.command.SearchCareOccurrencesQuery
import dev.vilquer.petcarescheduler.usecase.command.UndoCareOccurrenceCommand
import dev.vilquer.petcarescheduler.usecase.command.UpdateCarePlanCommand
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.CareOccurrenceActionRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.CareOccurrenceFilter
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.CareOccurrenceRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.CarePlanRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.CareReminderOutboxMessage
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.CareReminderOutboxPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.ClockPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PetRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TransactionPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.CareOccurrenceUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.CarePlanUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.CareScheduleMaintenanceUseCase
import dev.vilquer.petcarescheduler.usecase.result.CareOccurrenceResult
import dev.vilquer.petcarescheduler.usecase.result.CareOccurrencesPageResult
import dev.vilquer.petcarescheduler.usecase.result.CarePlanResult
import dev.vilquer.petcarescheduler.usecase.result.CarePlansPageResult
import dev.vilquer.petcarescheduler.usecase.result.TodayCareResult
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

class CareAppService(
    private val plans: CarePlanRepositoryPort,
    private val occurrences: CareOccurrenceRepositoryPort,
    private val actions: CareOccurrenceActionRepositoryPort,
    private val pets: PetRepositoryPort,
    private val tutors: TutorRepositoryPort,
    private val reminderOutbox: CareReminderOutboxPort,
    private val transaction: TransactionPort,
    private val clock: ClockPort,
) : CarePlanUseCase, CareOccurrenceUseCase, CareScheduleMaintenanceUseCase {

    override fun create(command: CreateCarePlanCommand, tutorId: TutorId): CarePlanResult {
        require(pets.existsForTutor(command.petId, tutorId)) { "care_plan_pet_not_owned" }
        val now = clock.now()
        require(!command.startAt.isBefore(now.toLocalDateTime().minusMinutes(5))) { "care_plan_start_in_past" }
        val plan = CarePlan(
            tutorId = tutorId,
            petId = command.petId,
            responsibleTutorId = tutorId,
            type = command.type,
            title = command.title.trim(),
            instructions = command.instructions?.trim()?.takeIf { it.isNotEmpty() },
            startAt = command.startAt,
            recurrence = command.recurrence,
            reminderMinutesBefore = command.reminderMinutesBefore,
            createdAt = now.toInstant(),
            updatedAt = now.toInstant(),
        )
        return transaction.execute {
            val saved = plans.save(plan)
            occurrences.saveAllIfAbsent(saved.occurrencesThrough(horizon(now.toLocalDateTime()), now.toInstant()))
            saved.toResult()
        }
    }

    override fun update(command: UpdateCarePlanCommand, tutorId: TutorId): CarePlanResult = transaction.execute {
        val existing = plans.findByIdAndTutorForUpdate(command.planId, tutorId)
            ?: throw NotFoundException("Plano de cuidado não encontrado")
        if (!existing.active) throw ConflictException("Este plano já foi encerrado")
        val now = clock.now()
        val updated = existing.copy(
            scheduleRevision = existing.scheduleRevision + 1,
            type = command.type,
            title = command.title.trim(),
            instructions = command.instructions?.trim()?.takeIf { it.isNotEmpty() },
            startAt = command.startAt,
            recurrence = command.recurrence,
            reminderMinutesBefore = command.reminderMinutesBefore,
            updatedAt = now.toInstant(),
        )
        occurrences.cancelScheduledFrom(existing.id, now.toLocalDateTime(), now.toInstant())
        val saved = plans.save(updated)
        occurrences.saveAllIfAbsent(
            saved.occurrencesThrough(horizon(now.toLocalDateTime()), now.toInstant())
                .filterNot { it.dueAt.isBefore(now.toLocalDateTime()) },
        )
        saved.toResult()
    }

    override fun deactivate(planId: CarePlanId, tutorId: TutorId) {
        transaction.execute {
            val plan = plans.findByIdAndTutorForUpdate(planId, tutorId)
                ?: throw NotFoundException("Plano de cuidado não encontrado")
            if (!plan.active) return@execute
            val now = clock.now().toInstant()
            occurrences.cancelAllScheduled(plan.id, now)
            plans.save(plan.deactivate(now))
        }
    }

    override fun get(planId: CarePlanId, tutorId: TutorId): CarePlanResult =
        plans.findByIdAndTutor(planId, tutorId)?.toResult()
            ?: throw NotFoundException("Plano de cuidado não encontrado")

    override fun list(tutorId: TutorId, petId: dev.vilquer.petcarescheduler.core.domain.entity.PetId?, active: Boolean?, page: Int, size: Int): CarePlansPageResult {
        validatePage(page, size)
        petId?.let { require(pets.existsForTutor(it, tutorId)) { "care_plan_pet_not_owned" } }
        return CarePlansPageResult(
            plans.listByTutor(tutorId, petId, active, page, size).map { it.toResult() },
            plans.countByTutor(tutorId, petId, active),
            page,
            size,
        )
    }

    override fun search(query: SearchCareOccurrencesQuery, tutorId: TutorId): CareOccurrencesPageResult {
        validatePage(query.page, query.size)
        require(query.to.isAfter(query.from)) { "care_period_invalid" }
        require(Duration.between(query.from, query.to).toDays() <= MAX_SEARCH_DAYS) { "care_period_too_large" }
        query.petId?.let { require(pets.existsForTutor(it, tutorId)) { "care_occurrence_pet_not_owned" } }
        val filter = CareOccurrenceFilter(query.from, query.to, query.petId, query.type, query.status)
        return CareOccurrencesPageResult(
            occurrences.search(tutorId, filter, query.page, query.size).map { it.toResult() },
            occurrences.count(tutorId, filter),
            query.page,
            query.size,
        )
    }

    override fun today(tutorId: TutorId): TodayCareResult {
        val now = clock.now()
        val start = now.toLocalDate().atStartOfDay()
        val end = start.plusDays(1)
        val overdueFilter = CareOccurrenceFilter(start.minusYears(2), start, status = CareOccurrenceStatus.SCHEDULED)
        val todayFilter = CareOccurrenceFilter(start, end)
        val overdue = occurrences.search(tutorId, overdueFilter, 0, TODAY_LIMIT).map { it.toResult() }
        val today = occurrences.search(tutorId, todayFilter, 0, TODAY_LIMIT)
        val scheduled = today.filter { it.status == CareOccurrenceStatus.SCHEDULED }.map { it.toResult() }
        val completed = today.filter { it.status == CareOccurrenceStatus.COMPLETED }.map { it.toResult() }
        val nextWeek = CareOccurrenceFilter(start, start.plusDays(7), status = CareOccurrenceStatus.SCHEDULED)
        return TodayCareResult(now.toLocalDate(), overdue, scheduled, completed, occurrences.count(tutorId, nextWeek))
    }

    override fun complete(command: CompleteCareOccurrenceCommand, tutorId: TutorId): CareOccurrenceResult = transaction.execute {
        replay(command.requestId, command.occurrenceId.value, tutorId, CareOccurrenceActionType.COMPLETE)?.let {
            return@execute it
        }
        require(command.note == null || command.note.length <= 500) { "care_occurrence_note_invalid" }
        val occurrence = occurrences.findByIdAndTutorForUpdate(command.occurrenceId, tutorId)
            ?: throw NotFoundException("Cuidado não encontrado")
        // Uma segunda requisição com a mesma chave pode ter ficado esperando o
        // lock da ocorrência. Revalidar depois do lock mantém a resposta
        // idempotente também sob concorrência real entre instâncias.
        replay(command.requestId, command.occurrenceId.value, tutorId, CareOccurrenceActionType.COMPLETE)?.let {
            return@execute it
        }
        if (occurrence.status == CareOccurrenceStatus.COMPLETED) {
            throw ConflictException("Este cuidado já foi concluído; a agenda foi atualizada para evitar registro em dobro")
        }
        if (occurrence.status != CareOccurrenceStatus.SCHEDULED) {
            throw ConflictException("Este cuidado não está mais disponível para conclusão")
        }
        val now = clock.now().toInstant()
        val completed = occurrences.save(occurrence.complete(tutorId, now, command.note))
        actions.save(
            CareOccurrenceAction(
                requestId = command.requestId,
                occurrenceId = occurrence.id,
                actorTutorId = tutorId,
                action = CareOccurrenceActionType.COMPLETE,
                previousStatus = occurrence.status,
                newStatus = completed.status,
                happenedAt = now,
            ),
        )
        completed.toResult()
    }

    override fun undo(command: UndoCareOccurrenceCommand, tutorId: TutorId): CareOccurrenceResult = transaction.execute {
        replay(command.requestId, command.occurrenceId.value, tutorId, CareOccurrenceActionType.UNDO)?.let {
            return@execute it
        }
        val occurrence = occurrences.findByIdAndTutorForUpdate(command.occurrenceId, tutorId)
            ?: throw NotFoundException("Cuidado não encontrado")
        replay(command.requestId, command.occurrenceId.value, tutorId, CareOccurrenceActionType.UNDO)?.let {
            return@execute it
        }
        if (occurrence.status != CareOccurrenceStatus.COMPLETED) {
            throw ConflictException("Este cuidado não está concluído")
        }
        val now = clock.now().toInstant()
        val reopened = try {
            occurrence.reopen(tutorId, now, UNDO_WINDOW)
        } catch (ex: IllegalArgumentException) {
            throw ConflictException("O prazo seguro para desfazer esta conclusão terminou")
        }
        occurrences.save(reopened)
        actions.save(
            CareOccurrenceAction(
                requestId = command.requestId,
                occurrenceId = occurrence.id,
                actorTutorId = tutorId,
                action = CareOccurrenceActionType.UNDO,
                previousStatus = occurrence.status,
                newStatus = reopened.status,
                happenedAt = now,
            ),
        )
        reopened.toResult()
    }

    override fun materializeAndEnqueueReminders() {
        val now = clock.now()
        var page = 0
        while (true) {
            val batch = plans.findActive(page, MATERIALIZATION_BATCH)
            if (batch.isEmpty()) break
            batch.forEach { plan ->
                transaction.execute {
                    val locked = plans.findByIdAndTutorForUpdate(plan.id, plan.tutorId) ?: return@execute
                    if (locked.active) {
                        occurrences.saveAllIfAbsent(
                            locked.occurrencesThrough(horizon(now.toLocalDateTime()), now.toInstant()),
                        )
                    }
                }
            }
            if (batch.size < MATERIALIZATION_BATCH) break
            page += 1
        }

        val localNow = now.toLocalDateTime()
        occurrences.findReminderCandidates(localNow.minusHours(12), localNow.plusDays(8), REMINDER_SCAN_LIMIT)
            .forEach { occurrence ->
                val plan = plans.findByIdAndTutor(occurrence.planId, occurrence.tutorId) ?: return@forEach
                val triggerAt = occurrence.dueAt.minusMinutes(plan.reminderMinutesBefore.toLong())
                if (triggerAt.isAfter(localNow)) return@forEach
                val tutor = tutors.findById(occurrence.tutorId) ?: return@forEach
                val pet = pets.findById(occurrence.petId) ?: return@forEach
                reminderOutbox.enqueueIfAbsent(
                    CareReminderOutboxMessage(
                        occurrenceId = occurrence.id,
                        tutorId = occurrence.tutorId,
                        tutorEmail = tutor.email.value,
                        petName = pet.name,
                        createdAt = now.toInstant(),
                    ),
                )
            }
    }

    private fun replay(requestId: java.util.UUID, occurrenceId: java.util.UUID, tutorId: TutorId, expected: CareOccurrenceActionType): CareOccurrenceResult? {
        val action = actions.findByRequestId(requestId) ?: return null
        if (action.occurrenceId.value != occurrenceId || action.actorTutorId != tutorId || action.action != expected) {
            throw ConflictException("A chave de idempotência já foi usada em outra operação")
        }
        return occurrences.findByIdAndTutor(action.occurrenceId, tutorId)?.toResult()
            ?: throw NotFoundException("Cuidado não encontrado")
    }

    private fun CarePlan.toResult() = CarePlanResult(
        id.value, version, petId.value, responsibleTutorId.value, type, title, instructions,
        startAt, recurrence, reminderMinutesBefore, active,
    )

    private fun CareOccurrence.toResult(): CareOccurrenceResult {
        val undoUntil = completedAt?.plus(UNDO_WINDOW)
        return CareOccurrenceResult(
            id.value, version, planId.value, petId.value, type, title, instructions, dueAt, status,
            completedAt, completedByTutorId?.value, completionNote, undoUntil,
        )
    }

    private fun validatePage(page: Int, size: Int) {
        require(page >= 0) { "page_invalid" }
        require(size in 1..100) { "size_invalid" }
    }

    private fun horizon(now: LocalDateTime) = now.plusDays(MATERIALIZATION_HORIZON_DAYS)

    companion object {
        val UNDO_WINDOW: Duration = Duration.ofMinutes(10)
        private const val MATERIALIZATION_HORIZON_DAYS = 90L
        private const val MATERIALIZATION_BATCH = 100
        private const val TODAY_LIMIT = 100
        private const val MAX_SEARCH_DAYS = 366L
        private const val REMINDER_SCAN_LIMIT = 500
    }
}
