package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrence
import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrenceAction
import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrenceId
import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrenceStatus
import dev.vilquer.petcarescheduler.core.domain.care.CarePlan
import dev.vilquer.petcarescheduler.core.domain.care.CarePlanId
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import java.time.LocalDateTime
import java.util.UUID

data class CareOccurrenceFilter(
    val from: LocalDateTime,
    val to: LocalDateTime,
    val petId: PetId? = null,
    val type: EventType? = null,
    val status: CareOccurrenceStatus? = null,
)

interface CarePlanRepositoryPort {
    fun save(plan: CarePlan): CarePlan
    fun findByIdAndTutor(id: CarePlanId, tutorId: TutorId): CarePlan?
    fun findByIdAndTutorForUpdate(id: CarePlanId, tutorId: TutorId): CarePlan?
    fun listByTutor(tutorId: TutorId, petId: PetId?, active: Boolean?, page: Int, size: Int): List<CarePlan>
    fun countByTutor(tutorId: TutorId, petId: PetId?, active: Boolean?): Long
    fun findActive(page: Int, size: Int): List<CarePlan>
}

interface CareOccurrenceRepositoryPort {
    fun save(occurrence: CareOccurrence): CareOccurrence
    fun saveAllIfAbsent(occurrences: List<CareOccurrence>): Int
    fun findByIdAndTutor(id: CareOccurrenceId, tutorId: TutorId): CareOccurrence?
    fun findByIdAndTutorForUpdate(id: CareOccurrenceId, tutorId: TutorId): CareOccurrence?
    fun search(tutorId: TutorId, filter: CareOccurrenceFilter, page: Int, size: Int): List<CareOccurrence>
    fun count(tutorId: TutorId, filter: CareOccurrenceFilter): Long
    fun cancelScheduledFrom(planId: CarePlanId, from: LocalDateTime, updatedAt: java.time.Instant): Int
    fun cancelAllScheduled(planId: CarePlanId, updatedAt: java.time.Instant): Int
    fun findReminderCandidates(from: LocalDateTime, to: LocalDateTime, limit: Int): List<CareOccurrence>
    fun countByTutor(tutorId: TutorId): Long
    fun findUpcoming(tutorId: TutorId, from: LocalDateTime, to: LocalDateTime, limit: Int): List<CareOccurrence>
}

interface CareOccurrenceActionRepositoryPort {
    fun findByRequestId(requestId: UUID): CareOccurrenceAction?
    fun save(action: CareOccurrenceAction): CareOccurrenceAction
}
