package dev.vilquer.petcarescheduler.application.mapper

import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.usecase.command.RegisterEventCommand
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class EventDtoMapper {

    data class RegisterRequest(
        val petId: Long,
        val type: String,          // "VACCINE", "BATH"…
        val description: String,
        /** ISO-8601 (ex.: 2025-07-01T09:00:00Z) */
        val dateStart: String
    )

    fun toRegisterCommand(dto: RegisterRequest): RegisterEventCommand =
        RegisterEventCommand(
            petId      = PetId(dto.petId),
            type       = EventType.valueOf(dto.type.uppercase()),
            description= dto.description,
            dateStart  = LocalDateTime.parse(dto.dateStart)
        )
}
