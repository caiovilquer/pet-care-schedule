package dev.vilquer.petcarescheduler.infra.adapter.output.external

import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrence
import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrenceAction
import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrenceId
import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrenceStatus
import dev.vilquer.petcarescheduler.core.domain.care.CarePlan
import dev.vilquer.petcarescheduler.core.domain.care.CarePlanId
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.toDomain
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.toJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.CareOccurrenceActionJpaRepository
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.CareOccurrenceJpaRepository
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.CarePlanJpaRepository
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.CareOccurrenceActionRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.CareOccurrenceFilter
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.CareOccurrenceRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.CarePlanRepositoryPort
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Repository
class CarePlanRepositoryAdapter(private val jpa: CarePlanJpaRepository) : CarePlanRepositoryPort {
    override fun save(plan: CarePlan) = jpa.save(plan.toJpa()).toDomain()
    override fun findByIdAndTutor(id: CarePlanId, tutorId: TutorId) = jpa.findOwned(id.value, tutorId.value)?.toDomain()
    override fun findByIdAndTutorForUpdate(id: CarePlanId, tutorId: TutorId) = jpa.findOwnedForUpdate(id.value, tutorId.value)?.toDomain()
    override fun listByTutor(tutorId: TutorId, petId: PetId?, active: Boolean?, page: Int, size: Int) =
        jpa.findOwnedPage(tutorId.value, petId?.value, active, PageRequest.of(page, size)).content.map { it.toDomain() }
    override fun countByTutor(tutorId: TutorId, petId: PetId?, active: Boolean?) =
        jpa.findOwnedPage(tutorId.value, petId?.value, active, PageRequest.of(0, 1)).totalElements
    override fun findActive(page: Int, size: Int) =
        jpa.findAllByActiveTrueOrderByUpdatedAtAsc(PageRequest.of(page, size)).map { it.toDomain() }
}

@Repository
class CareOccurrenceRepositoryAdapter(private val jpa: CareOccurrenceJpaRepository) : CareOccurrenceRepositoryPort {
    override fun save(occurrence: CareOccurrence) = jpa.save(occurrence.toJpa()).toDomain()
    override fun saveAllIfAbsent(occurrences: List<CareOccurrence>): Int {
        val missing = occurrences.filterNot {
            jpa.existsByPlanIdAndScheduleRevisionAndSequence(it.planId.value, it.scheduleRevision, it.sequence)
        }
        jpa.saveAll(missing.map { it.toJpa() })
        return missing.size
    }
    override fun findByIdAndTutor(id: CareOccurrenceId, tutorId: TutorId) = jpa.findOwned(id.value, tutorId.value)?.toDomain()
    override fun findByIdAndTutorForUpdate(id: CareOccurrenceId, tutorId: TutorId) = jpa.findOwnedForUpdate(id.value, tutorId.value)?.toDomain()
    override fun search(tutorId: TutorId, filter: CareOccurrenceFilter, page: Int, size: Int) =
        jpa.search(tutorId.value, filter.from, filter.to, filter.petId?.value, filter.type, filter.status, PageRequest.of(page, size))
            .content.map { it.toDomain() }
    override fun count(tutorId: TutorId, filter: CareOccurrenceFilter) =
        jpa.search(tutorId.value, filter.from, filter.to, filter.petId?.value, filter.type, filter.status, PageRequest.of(0, 1)).totalElements
    override fun cancelScheduledFrom(planId: CarePlanId, from: LocalDateTime, updatedAt: Instant) =
        jpa.cancelScheduledFrom(planId.value, from, CareOccurrenceStatus.SCHEDULED, CareOccurrenceStatus.CANCELLED, updatedAt)
    override fun cancelAllScheduled(planId: CarePlanId, updatedAt: Instant) =
        jpa.cancelAllScheduled(planId.value, CareOccurrenceStatus.SCHEDULED, CareOccurrenceStatus.CANCELLED, updatedAt)
    override fun findReminderCandidates(from: LocalDateTime, to: LocalDateTime, limit: Int) =
        jpa.findAllByStatusAndDueAtGreaterThanEqualAndDueAtLessThanOrderByDueAtAsc(
            CareOccurrenceStatus.SCHEDULED, from, to, PageRequest.of(0, limit),
        ).map { it.toDomain() }
    override fun countByTutor(tutorId: TutorId) = jpa.countByTutorId(tutorId.value)
    override fun findUpcoming(tutorId: TutorId, from: LocalDateTime, to: LocalDateTime, limit: Int) =
        jpa.findUpcoming(tutorId.value, CareOccurrenceStatus.SCHEDULED, from, to, PageRequest.of(0, limit)).map { it.toDomain() }
}

@Repository
class CareOccurrenceActionRepositoryAdapter(private val jpa: CareOccurrenceActionJpaRepository) : CareOccurrenceActionRepositoryPort {
    override fun findByRequestId(requestId: UUID) = jpa.findByRequestId(requestId)?.toDomain()
    override fun save(action: CareOccurrenceAction) = jpa.save(action.toJpa()).toDomain()
}
