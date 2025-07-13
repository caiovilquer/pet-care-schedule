package dev.vilquer.petcarescheduler.core.domain.entity

import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber

data class Tutor(
    val id: TutorId? = null,
    val firstName: String,
    val lastName: String?,
    val email: Email,
    val passwordHash: String,
    val phoneNumber: PhoneNumber? = null,
    val avatar: String? = null,
    val pets: List<Pet> = mutableListOf()
)

@JvmInline value class TutorId(val value: Long)