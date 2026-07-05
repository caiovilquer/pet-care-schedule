package dev.vilquer.petcarescheduler.usecase.command

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import java.time.LocalDate

data class CreatePetCommand(
    val name: String,
    val species: String,
    val breed: String?,
    val birthdate: LocalDate,
    val photoUrl: String?,
    val tutorId: TutorId
)