package dev.vilquer.petcarescheduler.core.domain.entity

import java.time.LocalDate
import java.util.UUID
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId

data class Pet(
    val id: PetId? = null,
    val version: Long? = null,
    val name: String,
    val species: String,
    val breed: String?,
    val birthdate: LocalDate?,
    val photoUrl: String? = null,
    val photoAssetId: UUID? = null,
    val tutorId: TutorId?,
    val householdId: HouseholdId? = null,
) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
        require(species.isNotBlank()) { "species must not be blank" }
    }
}

@JvmInline value class PetId(val value: Long)
