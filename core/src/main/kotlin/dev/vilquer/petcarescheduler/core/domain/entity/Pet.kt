package dev.vilquer.petcarescheduler.core.domain.entity

import java.time.LocalDate

data class Pet(
    val id: PetId? = null,
    val name: String,
    val species: String,
    val breed: String?,
    val birthdate: LocalDate?,
    val photoUrl: String? = null,
    val tutorId: TutorId?,
    val events: List<Event> = mutableListOf()
)

@JvmInline value class PetId(val value: Long)

