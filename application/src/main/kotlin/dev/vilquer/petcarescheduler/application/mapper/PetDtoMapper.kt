package dev.vilquer.petcarescheduler.application.mapper

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.command.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Past
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class PetDtoMapper {
    data class CreateRequest(
        @field:NotBlank val name: String,
        @field:NotBlank val specie: String,
        val race: String?,
        @field:Past val birthdate: LocalDate,
    )
    data class UpdateRequest(
        val name: String?,
        val race: String?,
        @field:Past val birthdate: LocalDate?
    )

    // === converters ===
    fun toCreateCommand(dto: CreateRequest, tutorId: TutorId): CreatePetCommand =
        CreatePetCommand(
            name      = dto.name,
            specie    = dto.specie,
            race      = dto.race,
            birthdate = dto.birthdate,
            tutorId   = tutorId
        )

    fun toUpdateCommand(id: Long, dto: UpdateRequest): UpdatePetCommand =
        UpdatePetCommand(
            petId      = PetId(id),
            name       = dto.name,
            race       = dto.race,
            birthdate = dto.birthdate
        )
}
