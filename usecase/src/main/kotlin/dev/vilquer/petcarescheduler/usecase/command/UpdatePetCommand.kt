package dev.vilquer.petcarescheduler.usecase.command

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import java.time.LocalDate

data class UpdatePetCommand(
    val petId: PetId,
    val name: String? = null,
    val race: String? = null,
    val birthdate: LocalDate? = null
)