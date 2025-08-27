package dev.vilquer.petcarescheduler.core.domain.reset

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import java.time.Instant
import java.util.UUID

data class PasswordResetToken (
    val id: UUID = UUID.randomUUID(),
    val tokenHash: String,
    val userId: TutorId,
    val expiresAt: Instant,
    val usedAt: Instant? = null,
    )