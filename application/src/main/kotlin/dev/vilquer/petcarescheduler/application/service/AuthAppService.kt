package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.exception.InvalidCredentialsException
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.session.RefreshToken
import dev.vilquer.petcarescheduler.usecase.command.LoginCommand
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PasswordHashPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.RefreshTokenPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TokenIssuerPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TransactionPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.AuthTokens
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.AuthUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.SessionUseCase
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID

class AuthAppService(
    private val tutorRepo: TutorRepositoryPort,
    private val passwordHash: PasswordHashPort,
    private val tokenIssuer: TokenIssuerPort,
    private val refreshTokens: RefreshTokenPort,
    private val transactionPort: TransactionPort,
    private val clock: Clock = Clock.systemUTC(),
    private val refreshTtl: Duration = Duration.ofDays(30),
    private val passwordChangeSkew: Duration = Duration.ofMinutes(2),
) : AuthUseCase, SessionUseCase {

    override fun authenticate(cmd: LoginCommand): AuthTokens {
        val tutor = tutorRepo.findByEmail(cmd.email)
            ?: throw InvalidCredentialsException("Invalid e-mail or password")

        if (!passwordHash.matches(cmd.rawPassword, tutor.passwordHash)) {
            throw InvalidCredentialsException("Invalid e-mail or password")
        }

        val userId = tutor.id!!
        val accessToken = tokenIssuer.issueAccessToken(tutor)
        val issued = issueRefreshToken(userId, UUID.randomUUID())
        return AuthTokens(accessToken, issued.first, issued.second.expiresAt)
    }

    override fun refresh(rawRefreshToken: String, userAgent: String?): AuthTokens {
        val now = Instant.now(clock)
        val current = refreshTokens.findByHash(sha256Hex(rawRefreshToken))
            ?: throw InvalidCredentialsException("Invalid refresh token")

        if (current.usedAt != null || current.revokedAt != null) {
            // Token já rotacionado ou revogado sendo reapresentado: indício de roubo,
            // então mata toda a família em vez de só recusar este token.
            refreshTokens.revokeFamily(current.familyId, now)
            throw InvalidCredentialsException("Refresh token reuse detected")
        }

        if (!current.expiresAt.isAfter(now)) {
            throw InvalidCredentialsException("Refresh token expired")
        }

        val passwordChangedAt = tutorRepo.findPasswordChangedAt(current.userId)
        if (passwordChangedAt != null && current.createdAt.isBefore(passwordChangedAt.minus(passwordChangeSkew))) {
            refreshTokens.revokeFamily(current.familyId, now)
            throw InvalidCredentialsException("Refresh token invalidated by password change")
        }

        val tutor = tutorRepo.findById(current.userId)
            ?: throw InvalidCredentialsException("Invalid refresh token")

        val accessToken = tokenIssuer.issueAccessToken(tutor)
        val (newTokenPlain, newToken) = transactionPort.execute {
            val issued = issueRefreshToken(current.userId, current.familyId, userAgent)
            refreshTokens.markRotated(current.id, issued.second.id, now)
            issued
        }
        return AuthTokens(accessToken, newTokenPlain, newToken.expiresAt)
    }

    override fun logout(rawRefreshToken: String) {
        val now = Instant.now(clock)
        val current = refreshTokens.findByHash(sha256Hex(rawRefreshToken)) ?: return
        refreshTokens.revokeFamily(current.familyId, now)
    }

    override fun logoutAll(userId: TutorId) {
        refreshTokens.revokeAllForUser(userId, Instant.now(clock))
    }

    private fun issueRefreshToken(
        userId: TutorId,
        familyId: UUID,
        userAgent: String? = null,
    ): Pair<String, RefreshToken> {
        val now = Instant.now(clock)
        val tokenPlain = generateToken()
        val created = refreshTokens.create(
            RefreshToken(
                tokenHash = sha256Hex(tokenPlain),
                userId = userId,
                familyId = familyId,
                expiresAt = now.plus(refreshTtl),
                createdAt = now,
                userAgent = userAgent,
            )
        )
        return tokenPlain to created
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
