package dev.vilquer.petcarescheduler.usecase.command

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import java.time.LocalDate

data class CreatePetCommand(
    val name: String,
    val specie: String,
    val race: String?,
    val birthdate: LocalDate,
    val tutorId: TutorId
)