package dev.vilquer.petcarescheduler.usecase.command

import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.valueobject.Frequency
import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import java.time.LocalDateTime

data class UpdateEventCommand(
    val eventId: EventId,
    val type: EventType? = null,
    val dateStart: LocalDateTime? = null,
    val description: String? = null,
    val frequency: Frequency? = null,
    val intervalCount: Long? = null,
    val repetitions: Int? = null,
    val finalDate: LocalDateTime? = null,
)