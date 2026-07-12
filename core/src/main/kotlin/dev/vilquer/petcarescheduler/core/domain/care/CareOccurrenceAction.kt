package dev.vilquer.petcarescheduler.core.domain.care

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import java.time.Instant
import java.util.UUID

enum class CareOccurrenceActionType { COMPLETE, UNDO, CANCEL }

data class CareOccurrenceAction(
    val id: UUID = UUID.randomUUID(),
    val requestId: UUID,
    val occurrenceId: CareOccurrenceId,
    val actorTutorId: TutorId,
    val action: CareOccurrenceActionType,
    val previousStatus: CareOccurrenceStatus,
    val newStatus: CareOccurrenceStatus,
    val happenedAt: Instant,
)
