package dev.vilquer.petcarescheduler.application.mapper

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.command.*
import org.mapstruct.*
import java.time.LocalDate

@Mapper(componentModel = "spring")
interface PetDtoMapper {
    data class CreateRequest(
        val name: String,
        val specie: String,
        val race: String?,
        val birthdate: String,
        val tutorId: Long
    )
    data class UpdateRequest(
        val name: String?,
        val race: String?,
        val birthdate: String?
    )

    // === converters ===
    fun toCreateCommand(dto: CreateRequest): CreatePetCommand =
        CreatePetCommand(
            name      = dto.name,
            specie    = dto.specie,
            race      = dto.race,
            birthdate = LocalDate.parse(dto.birthdate),
            tutorId   = TutorId(dto.tutorId)
        )

    fun toUpdateCommand(id: Long, dto: UpdateRequest): UpdatePetCommand =
        UpdatePetCommand(
            petId      = PetId(id),
            name       = dto.name,
            race       = dto.race,
            birthdate  = dto.birthdate?.let(LocalDate::parse)
        )
}
