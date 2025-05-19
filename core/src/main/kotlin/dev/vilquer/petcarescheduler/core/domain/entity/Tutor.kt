package dev.vilquer.petcarescheduler.core.domain.entity

data class Tutor(
    val id: TutorId? = null,
    val firstName: String,
    val lastName: String?,
    val email: String,
    val passwordHash: String,
    val phoneNumber: String,
    val avatar: String? = null,
    val pets: List<Pet> = mutableListOf()
)

@JvmInline value class TutorId(val value: Long)