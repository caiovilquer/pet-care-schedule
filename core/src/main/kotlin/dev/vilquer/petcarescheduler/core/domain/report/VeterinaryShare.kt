package dev.vilquer.petcarescheduler.core.domain.report

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@JvmInline value class VeterinaryShareId(val value: UUID)

data class VeterinaryShareScope(
    val from: LocalDate,
    val to: LocalDate,
    val includeNotes: Boolean = false,
    val includeCosts: Boolean = false,
    val includeDocuments: Boolean = false,
) {
    init {
        require(!to.isBefore(from)) { "veterinary_share_period_invalid" }
        require(Duration.between(from.atStartOfDay(), to.plusDays(1).atStartOfDay()) <= MAX_PERIOD) {
            "veterinary_share_period_too_large"
        }
    }

    companion object { private val MAX_PERIOD = Duration.ofDays(366) }
}

data class VeterinaryShare(
    val id: VeterinaryShareId = VeterinaryShareId(UUID.randomUUID()),
    val version: Long? = null,
    val householdId: HouseholdId,
    val petId: PetId,
    val createdByTutorId: TutorId,
    val label: String,
    val tokenHash: String,
    val scope: VeterinaryShareScope,
    val expiresAt: Instant,
    val revokedAt: Instant? = null,
    val createdAt: Instant,
    val lastAccessedAt: Instant? = null,
    val accessCount: Long = 0,
) {
    init {
        require(label.isNotBlank() && label.length <= 100) { "veterinary_share_label_invalid" }
        require(tokenHash.matches(Regex("^[a-f0-9]{64}$"))) { "veterinary_share_hash_invalid" }
        require(expiresAt.isAfter(createdAt) && Duration.between(createdAt, expiresAt) <= MAX_TTL) {
            "veterinary_share_expiry_invalid"
        }
        require(accessCount >= 0) { "veterinary_share_access_count_invalid" }
    }

    fun revoke(at: Instant) = copy(revokedAt = at)
    fun accessed(at: Instant) = copy(lastAccessedAt = at, accessCount = accessCount + 1)

    companion object { private val MAX_TTL = Duration.ofDays(30) }
}
