package dev.vilquer.petcarescheduler.usecase.result

import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.Status
import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import java.time.LocalDateTime

data class EventDetailResult(
    val id: EventId,
    val type: EventType,
    val description: String?,
    val dateStart: LocalDateTime,
    val recurrence: Recurrence? = null,
    val status: Status,
)