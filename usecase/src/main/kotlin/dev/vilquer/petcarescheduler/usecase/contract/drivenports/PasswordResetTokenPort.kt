package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.reset.PasswordResetToken
import java.time.Instant
import java.util.UUID

interface PasswordResetTokenPort {
    fun create (token: PasswordResetToken): PasswordResetToken
    fun findActiveByHash (tokenHash: String): PasswordResetToken?
    fun markUsed (id: UUID, usedAt: Instant)
    fun cleanup (now: Instant)
}