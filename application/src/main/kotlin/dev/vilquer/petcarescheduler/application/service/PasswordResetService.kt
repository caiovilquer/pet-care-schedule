package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.core.domain.reset.PasswordResetToken
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PasswordHashPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PasswordResetNotifierPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PasswordResetTokenPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TransactionPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.PasswordResetUseCase
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64

class PasswordResetService(
    private val tutors: TutorRepositoryPort,
    private val tokens: PasswordResetTokenPort,
    private val notifier: PasswordResetNotifierPort,
    private val passwordHash: PasswordHashPort,
    private val transactionPort: TransactionPort,
    private val clock: Clock = Clock.systemUTC(),
    private val ttlMinutes: Long = 30,
) : PasswordResetUseCase {

    override fun requestReset(email: Email) {
        val tutor = tutors.findByEmail(email) ?: return // resposta neutra
        val now = Instant.now(clock)
        val userId = tutor.id ?: throw IllegalStateException("User not found")
        val tokenPlain = generateToken()
        val tokenHash = sha256Hex(tokenPlain)
        val ttl = Duration.ofMinutes(ttlMinutes)
        val expires = now.plus(ttl)

        // Invalidar tokens antigos e criar o novo é uma unidade: uma queda no
        // meio deixaria o tutor sem nenhum token ativo e sem saber.
        transactionPort.execute {
            tokens.invalidateAllForUser(userId, now)
            tokens.create(
                PasswordResetToken(
                    tokenHash = tokenHash,
                    userId = userId,
                    expiresAt = expires
                )
            )
        }

        // Fora da transação: chamada de rede não deve segurar conexão de banco aberta.
        notifier.sendResetLink(tutor.email, tokenPlain, ttl)
    }

    override fun validate(token: String): Boolean {
        val t = tokens.findActiveByHash(sha256Hex(token)) ?: return false
        return t.expiresAt.isAfter(Instant.now(clock))
    }

    override fun reset(token: String, newPassword: String) {
        val now = Instant.now(clock)
        val t = tokens.findActiveByHash(sha256Hex(token))
            ?: throw IllegalArgumentException("invalid_token")
        if (t.expiresAt.isBefore(now)) throw IllegalArgumentException("expired_token")

        val tutor = tutors.findById(t.userId) ?: throw IllegalStateException("user_not_found")

        // Trocar a senha, atualizar passwordChangedAt (invalida JWTs antigos) e
        // marcar o token usado precisam ser tudo-ou-nada: uma falha no meio
        // deixaria a senha trocada mas o token ainda "ativo", reutilizável.
        transactionPort.execute {
            tutors.updatePassword(tutor.id!!, passwordHash.hash(newPassword))
            tutors.bumpPasswordChangedAt(tutor.id!!, now)
            tokens.markUsed(t.id, now)
        }
    }

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
    private fun sha256Hex(s: String): String {
        val dig = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(StandardCharsets.UTF_8))
        return buildString(dig.size * 2) { dig.forEach { append("%02x".format(it)) } }
    }
}
