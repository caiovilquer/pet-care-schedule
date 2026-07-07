package dev.vilquer.petcarescheduler.core.domain.session

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import java.time.Instant
import java.util.UUID

data class RefreshToken(
    val id: UUID = UUID.randomUUID(),
    val tokenHash: String,
    val userId: TutorId,
    val familyId: UUID,
    val expiresAt: Instant,
    val createdAt: Instant = Instant.now(),
    val usedAt: Instant? = null,
    val revokedAt: Instant? = null,
    val replacedBy: UUID? = null,
    val userAgent: String? = null,
) {
    fun isActive(now: Instant): Boolean =
        usedAt == null && revokedAt == null && expiresAt.isAfter(now)
}
