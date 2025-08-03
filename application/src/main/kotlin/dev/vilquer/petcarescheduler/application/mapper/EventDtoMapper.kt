package dev.vilquer.petcarescheduler.application.mapper

import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Frequency
import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import dev.vilquer.petcarescheduler.usecase.command.RegisterEventCommand
import dev.vilquer.petcarescheduler.usecase.command.UpdateEventCommand
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class EventDtoMapper {

    data class RegisterRequest(
        @field:Positive val petId: Long,
        @field:NotBlank val type: String,          // "VACCINE", "BATH"…
        @field:NotBlank val description: String,
        /** ISO-8601 (ex.: 2025-07-01T09:00:00Z) */
        @field:FutureOrPresent val dateStart: LocalDateTime,
        val frequency: Frequency?,
        @field:Positive val intervalCount: Long = 1,
        @field:Positive val repetitions: Int? = null,
        @field:FutureOrPresent val finalDate: LocalDateTime? = null
    )
    data class UpdateRequest(
        @field:NotBlank val description: String,
        @field:FutureOrPresent val dateStart: LocalDateTime,
        val frequency: Frequency?,
        @field:Positive val intervalCount: Long = 1,
        @field:Positive val repetitions: Int? = null,
        @field:FutureOrPresent val finalDate: LocalDateTime? = null
    )

    fun toRegisterCommand(dto: RegisterRequest): RegisterEventCommand =
        RegisterEventCommand(
            petId      = PetId(dto.petId),
            type       = EventType.valueOf(dto.type.uppercase()),
            description= dto.description,
            dateStart  = dto.dateStart,
            recurrence = if (dto.frequency != null) Recurrence(dto.frequency, dto.intervalCount,dto.repetitions, dto.finalDate) else null
        )
    fun toUpdateCommand(id: Long, dto: UpdateRequest): UpdateEventCommand =
        UpdateEventCommand(
            eventId      = EventId(id),
            dateStart   = dto.dateStart,
            description = dto.description,
            recurrence = if (dto.frequency != null) Recurrence(dto.frequency, dto.intervalCount ,dto.repetitions, dto.finalDate) else null
        )
}
