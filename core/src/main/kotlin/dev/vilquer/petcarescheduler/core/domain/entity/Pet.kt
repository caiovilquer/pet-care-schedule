package dev.vilquer.petcarescheduler.core.domain.entity

import java.time.LocalDate

data class Pet(
    val id: PetId? = null,
    val name: String,
    val specie: String,
    val race: String?,
    val birthdate: LocalDate?,
    val tutorId: TutorId,
    val events: List<Event> = mutableListOf()
)

@JvmInline value class PetId(val value: Long)

