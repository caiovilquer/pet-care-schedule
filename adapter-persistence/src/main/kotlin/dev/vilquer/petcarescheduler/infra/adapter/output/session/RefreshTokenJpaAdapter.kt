package dev.vilquer.petcarescheduler.infra.adapter.output.session

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.session.RefreshToken
import dev.vilquer.petcarescheduler.infra.adapter.output.session.jpa.RefreshTokenJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.session.jpa.RefreshTokenRepository
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.RefreshTokenPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Component
class RefreshTokenJpaAdapter(
    private val repo: RefreshTokenRepository
) : RefreshTokenPort {

    override fun create(token: RefreshToken): RefreshToken =
        repo.save(token.toJpa()).toDomain()

    override fun findByHash(tokenHash: String): RefreshToken? =
        repo.findByTokenHash(tokenHash)?.toDomain()

    @Transactional
    override fun markRotated(id: UUID, replacedBy: UUID, at: Instant) {
        val jpa = repo.findById(id).orElseThrow()
        jpa.usedAt = at
        jpa.replacedBy = replacedBy
        repo.save(jpa)
    }

    @Transactional
    override fun revokeFamily(familyId: UUID, at: Instant) {
        repo.revokeByFamilyId(familyId, at)
    }

    @Transactional
    override fun revokeAllForUser(userId: TutorId, at: Instant) {
        repo.revokeByUserId(userId.value, at)
    }

    @Transactional
    override fun cleanup(now: Instant) {
        repo.deleteExpiredOrRevoked(now)
    }

    private fun RefreshToken.toJpa() = RefreshTokenJpa(
        id = id,
        tokenHash = tokenHash,
        userId = userId.value,
        familyId = familyId,
        expiresAt = expiresAt,
        createdAt = createdAt,
        usedAt = usedAt,
        revokedAt = revokedAt,
        replacedBy = replacedBy,
        userAgent = userAgent
    )

    private fun RefreshTokenJpa.toDomain() = RefreshToken(
        id = id,
        tokenHash = tokenHash,
        userId = userId?.let { TutorId(it) } ?: throw IllegalStateException("User not found"),
        familyId = familyId,
        expiresAt = expiresAt,
        createdAt = createdAt,
        usedAt = usedAt,
        revokedAt = revokedAt,
        replacedBy = replacedBy,
        userAgent = userAgent
    )
}
