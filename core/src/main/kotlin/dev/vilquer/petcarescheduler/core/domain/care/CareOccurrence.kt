package dev.vilquer.petcarescheduler.core.domain.care

import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@JvmInline value class CareOccurrenceId(val value: UUID)

enum class CareOccurrenceStatus { SCHEDULED, COMPLETED, CANCELLED }

data class CareOccurrence(
    val id: CareOccurrenceId,
    val version: Long? = null,
    val planId: CarePlanId,
    val scheduleRevision: Int,
    val tutorId: TutorId,
    val petId: PetId,
    val sequence: Int,
    val type: EventType,
    val title: String,
    val instructions: String? = null,
    val dueAt: LocalDateTime,
    val status: CareOccurrenceStatus,
    val completedAt: Instant? = null,
    val completedByTutorId: TutorId? = null,
    val completionNote: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val legacyEventId: Long? = null,
) {
    init {
        require(sequence >= 0) { "care_occurrence_sequence_invalid" }
        require(scheduleRevision >= 0) { "care_occurrence_revision_invalid" }
        require(title.isNotBlank() && title.length <= 120) { "care_occurrence_title_invalid" }
        require(completionNote == null || completionNote.length <= 500) { "care_occurrence_note_invalid" }
        require(
            status != CareOccurrenceStatus.COMPLETED ||
                (completedAt != null && completedByTutorId != null),
        ) { "care_occurrence_completion_incomplete" }
    }

    fun complete(actor: TutorId, at: Instant, note: String?): CareOccurrence {
        require(status == CareOccurrenceStatus.SCHEDULED) { "care_occurrence_not_scheduled" }
        return copy(
            status = CareOccurrenceStatus.COMPLETED,
            completedAt = at,
            completedByTutorId = actor,
            completionNote = note?.trim()?.takeIf { it.isNotEmpty() },
            updatedAt = at,
        )
    }

    fun reopen(actor: TutorId, at: Instant, undoWindow: Duration): CareOccurrence {
        require(status == CareOccurrenceStatus.COMPLETED) { "care_occurrence_not_completed" }
        require(completedByTutorId == actor) { "care_occurrence_undo_actor_mismatch" }
        require(completedAt != null && !at.isAfter(completedAt.plus(undoWindow))) { "care_occurrence_undo_expired" }
        return copy(
            status = CareOccurrenceStatus.SCHEDULED,
            completedAt = null,
            completedByTutorId = null,
            completionNote = null,
            updatedAt = at,
        )
    }

    fun cancel(at: Instant): CareOccurrence {
        require(status == CareOccurrenceStatus.SCHEDULED) { "care_occurrence_not_scheduled" }
        return copy(status = CareOccurrenceStatus.CANCELLED, updatedAt = at)
    }
}
