package dev.vilquer.petcarescheduler.usecase.command

import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrenceId
import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrenceStatus
import dev.vilquer.petcarescheduler.core.domain.care.CarePlanId
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import java.time.LocalDateTime
import java.util.UUID

data class CreateCarePlanCommand(
    val petId: PetId,
    val type: EventType,
    val title: String,
    val instructions: String?,
    val startAt: LocalDateTime,
    val recurrence: Recurrence?,
    val reminderMinutesBefore: Int,
    val responsibleTutorId: TutorId? = null,
    val critical: Boolean = false,
    val escalationDelayMinutes: Int? = null,
    val escalationTutorId: TutorId? = null,
)

data class UpdateCarePlanCommand(
    val planId: CarePlanId,
    val type: EventType,
    val title: String,
    val instructions: String?,
    val startAt: LocalDateTime,
    val recurrence: Recurrence?,
    val reminderMinutesBefore: Int,
    val responsibleTutorId: TutorId? = null,
    val critical: Boolean = false,
    val escalationDelayMinutes: Int? = null,
    val escalationTutorId: TutorId? = null,
)

data class AssignCareOccurrenceCommand(
    val occurrenceId: CareOccurrenceId,
    val expectedVersion: Long,
    val responsibleTutorId: TutorId,
)

data class CompleteCareOccurrenceCommand(
    val occurrenceId: CareOccurrenceId,
    val requestId: UUID,
    val note: String?,
)

data class UndoCareOccurrenceCommand(
    val occurrenceId: CareOccurrenceId,
    val requestId: UUID,
)

data class SearchCareOccurrencesQuery(
    val from: LocalDateTime,
    val to: LocalDateTime,
    val petId: PetId? = null,
    val type: EventType? = null,
    val status: CareOccurrenceStatus? = null,
    val page: Int = 0,
    val size: Int = 20,
)
