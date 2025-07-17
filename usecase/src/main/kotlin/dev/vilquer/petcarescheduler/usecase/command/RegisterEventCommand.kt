package dev.vilquer.petcarescheduler.usecase.command

import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import java.time.LocalDateTime

data class RegisterEventCommand(
    val petId: PetId,
    val type: EventType,
    val description: String,
    val dateStart: LocalDateTime,
    val recurrence: Recurrence? = null
)