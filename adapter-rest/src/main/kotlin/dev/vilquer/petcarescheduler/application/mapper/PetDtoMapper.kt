package dev.vilquer.petcarescheduler.application.mapper

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.command.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class PetDtoMapper {
    data class CreateRequest(
        @field:NotBlank @field:Size(max = 80) val name: String,
        @field:NotBlank @field:Size(max = 40) val species: String,
        @field:Size(max = 80) val breed: String?,
        @field:PastOrPresent val birthdate: LocalDate?,
        @field:Size(max = 512)
        @field:Pattern(regexp = "^(https?://\\S+)?$", message = "deve ser uma URL HTTP(S) válida")
        val photoUrl: String? = null,
    )
    data class UpdateRequest(
        @field:NotBlank @field:Size(max = 80) val name: String,
        @field:Size(max = 80) val breed: String?,
        @field:PastOrPresent val birthdate: LocalDate?,
        @field:Size(max = 512)
        @field:Pattern(regexp = "^(https?://\\S+)?$", message = "deve ser uma URL HTTP(S) válida")
        val photoUrl: String?,
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
