package dev.vilquer.petcarescheduler.core.domain.entity

data class Pet(
    val id: PetId? = null,
    val name: String,
    val specie: String = "",
    val race: String?,
    val birthdate: String?,
    val tutor: Tutor,
    val event: List<Event> = mutableListOf()
)

@JvmInline value class PetId(val value: Long)

