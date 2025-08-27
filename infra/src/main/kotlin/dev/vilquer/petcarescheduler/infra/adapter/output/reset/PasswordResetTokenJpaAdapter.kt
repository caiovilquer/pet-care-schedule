package dev.vilquer.petcarescheduler.infra.adapter.output.reset

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.reset.PasswordResetToken
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PasswordResetTokenPort
import dev.vilquer.petcarescheduler.infra.adapter.output.reset.jpa.PasswordResetTokenJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.reset.jpa.PasswordResetTokenRepository
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class PasswordResetTokenJpaAdapter(
    private val repo: PasswordResetTokenRepository
) : PasswordResetTokenPort {

    override fun create(token: PasswordResetToken): PasswordResetToken =
        repo.save(token.toJpa()).toDomain()

    override fun findActiveByHash(tokenHash: String): PasswordResetToken? =
        repo.findByTokenHashAndUsedAtIsNull(tokenHash)
            ?.takeIf { it.expiresAt.isAfter(Instant.now()) }
            ?.toDomain()

    override fun markUsed(id: UUID, usedAt: Instant) {
        val jpa = repo.findById(id).orElseThrow()
        jpa.usedAt = usedAt
        repo.save(jpa)
    }

    override fun cleanup(now: Instant) {
        // opcional: criar query @Modifying pra limpar expirados + usados
        repo.findAll().filter { it.usedAt != null || it.expiresAt.isBefore(now) }
            .forEach { repo.delete(it) }
    }

    private fun PasswordResetToken.toJpa() = PasswordResetTokenJpa(
        id = id,
        tokenHash = tokenHash,
        userId = userId.value,
        expiresAt = expiresAt,
        usedAt = usedAt
    )

    private fun PasswordResetTokenJpa.toDomain() = PasswordResetToken(
        id = id,
        tokenHash = tokenHash,
        userId = userId?.let { TutorId(it) } ?: throw IllegalStateException("User not found"),
        expiresAt = expiresAt,
        usedAt = usedAt
    )
}
