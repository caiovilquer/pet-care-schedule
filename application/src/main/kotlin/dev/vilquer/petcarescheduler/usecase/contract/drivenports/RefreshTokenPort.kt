package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.session.RefreshToken
import java.time.Instant
import java.util.UUID

interface RefreshTokenPort {
    fun create(token: RefreshToken): RefreshToken

    /** Devolve o token em qualquer estado (usado, revogado ou ativo) — necessário para detectar reuso. */
    fun findByHash(tokenHash: String): RefreshToken?
    fun markRotated(id: UUID, replacedBy: UUID, at: Instant)
    fun revokeFamily(familyId: UUID, at: Instant)
    fun revokeAllForUser(userId: TutorId, at: Instant)
    fun cleanup(now: Instant)
}
