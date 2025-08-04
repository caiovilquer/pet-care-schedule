package dev.vilquer.petcarescheduler.usecase.result

import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.Status
import java.time.LocalDateTime

data class EventSummary(
    val id: EventId,
    val type: EventType,
    val description: String?,
    val status: Status,
    val dateStart: LocalDateTime,
    val petId: PetId
)