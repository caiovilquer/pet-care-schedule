package dev.vilquer.petcarescheduler.core.domain.entity

import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber

data class Tutor(
    val id: TutorId? = null,
    val firstName: String,
    val lastName: String?,
    val email: Email,
    val passwordHash: String,
    val passwordChangedAt: java.time.Instant? = null,
    val phoneNumber: PhoneNumber? = null,
    val avatar: String? = null
) {
    init {
        require(firstName.isNotBlank()) { "firstName must not be blank" }
    }
}

@JvmInline value class TutorId(val value: Long)
