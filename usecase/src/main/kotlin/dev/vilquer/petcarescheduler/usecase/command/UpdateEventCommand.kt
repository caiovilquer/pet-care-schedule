package dev.vilquer.petcarescheduler.usecase.command

import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import java.time.LocalDateTime

data class UpdateEventCommand(
    val eventId: EventId,
    val dateStart: LocalDateTime?,
    val description: String?,
    val recurrence: Recurrence? = null,
)