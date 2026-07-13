package dev.vilquer.petcarescheduler.core.domain.entity

import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber
import java.util.UUID
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId

data class Tutor(
    val id: TutorId? = null,
    val version: Long? = null,
    val firstName: String,
    val lastName: String?,
    val email: Email,
    val passwordHash: String,
    val passwordChangedAt: java.time.Instant? = null,
    val phoneNumber: PhoneNumber? = null,
    val avatar: String? = null,
    val avatarAssetId: UUID? = null,
    val defaultHouseholdId: HouseholdId? = null,
) {
    init {
        require(firstName.isNotBlank()) { "firstName must not be blank" }
    }
}

@JvmInline value class TutorId(val value: Long)
