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
        @field:NotBlank val species: String,
        val breed: String?,
        @field:Past val birthdate: LocalDate,
        val photoUrl: String? = null,
    )
    data class UpdateRequest(
        val name: String?,
        val breed: String?,
        @field:Past val birthdate: LocalDate?,
        val photoUrl: String? = null,
    )

    // === converters ===
    fun toCreateCommand(dto: CreateRequest, tutorId: TutorId): CreatePetCommand =
        CreatePetCommand(
            name      = dto.name,
            species   = dto.species,
            breed     = dto.breed,
            birthdate = dto.birthdate,
            photoUrl  = dto.photoUrl,
            tutorId   = tutorId
        )

    fun toUpdateCommand(id: Long, dto: UpdateRequest): UpdatePetCommand =
        UpdatePetCommand(
            petId      = PetId(id),
            name       = dto.name,
            breed      = dto.breed,
            birthdate = dto.birthdate,
            photoUrl  = dto.photoUrl
        )
}
