package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.FakePasswordHash
import dev.vilquer.petcarescheduler.application.FakeTransactionPort
import dev.vilquer.petcarescheduler.application.InMemoryRefreshTokenPort
import dev.vilquer.petcarescheduler.application.InMemoryTutorRepo
import dev.vilquer.petcarescheduler.application.exception.InvalidCredentialsException
import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber
import dev.vilquer.petcarescheduler.usecase.command.LoginCommand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class AuthAppServiceTest {

    private val tutorRepo = InMemoryTutorRepo()
    private val passwordHash = FakePasswordHash()
    private val refreshTokens = InMemoryRefreshTokenPort()
    private val transactionPort = FakeTransactionPort()
    private val tokenIssuer = { tutor: Tutor -> "access-for-${tutor.id!!.value}" }
    private val service = AuthAppService(
        tutorRepo, passwordHash, tokenIssuer, refreshTokens, transactionPort,
        refreshTtl = Duration.ofDays(30),
    )

    private fun savedTutor() = tutorRepo.save(
        Tutor(
            firstName = "Ana",
            lastName = null,
            email = Email.of("ana@example.com").getOrThrow(),
            passwordHash = passwordHash.hash("secret123"),
            phoneNumber = PhoneNumber.of("+5511912345678").getOrNull()
        )
    )

    @Test
    fun `authenticate issues an access token and a refresh token`() {
        val tutor = savedTutor()

        val tokens = service.authenticate(LoginCommand(tutor.email, "secret123"))

        assertEquals("access-for-${tutor.id!!.value}", tokens.accessToken)
        assertEquals(1, refreshTokens.allTokens().size)
    }

    @Test
    fun `authenticate rejects wrong password`() {
        val tutor = savedTutor()
        assertThrows(InvalidCredentialsException::class.java) {
            service.authenticate(LoginCommand(tutor.email, "wrong-password"))
        }
    }

    @Test
    fun `refresh rotates the token and keeps the same family`() {
        val tutor = savedTutor()
        val first = service.authenticate(LoginCommand(tutor.email, "secret123"))

        val second = service.refresh(first.refreshToken)

        assertNotEquals(first.refreshToken, second.refreshToken)
        val firstStored = refreshTokens.allTokens().first { it.tokenHash == sha256(first.refreshToken) }
        assertNotEquals(null, firstStored.usedAt)
        val secondStored = refreshTokens.allTokens().first { it.tokenHash == sha256(second.refreshToken) }
        assertEquals(firstStored.familyId, secondStored.familyId)
    }

    @Test
    fun `refresh reuse of a rotated token revokes the whole family`() {
        val tutor = savedTutor()
        val first = service.authenticate(LoginCommand(tutor.email, "secret123"))
        service.refresh(first.refreshToken)

        assertThrows(InvalidCredentialsException::class.java) {
            service.refresh(first.refreshToken)
        }

        // A rotação legítima também deve estar revogada agora — família inteira morta.
        val allInFamily = refreshTokens.allTokens().filter {
            it.familyId == refreshTokens.allTokens().first { t -> t.tokenHash == sha256(first.refreshToken) }.familyId
        }
        assertEquals(true, allInFamily.all { it.revokedAt != null })
    }

    @Test
    fun `refresh rejects unknown token`() {
        assertThrows(InvalidCredentialsException::class.java) {
            service.refresh("not-a-real-token")
        }
    }

    @Test
    fun `refresh rejects token issued before a password change`() {
        val tutor = savedTutor()
        val first = service.authenticate(LoginCommand(tutor.email, "secret123"))

        tutorRepo.bumpPasswordChangedAt(tutor.id!!, Instant.now().plus(Duration.ofMinutes(10)))

        assertThrows(InvalidCredentialsException::class.java) {
            service.refresh(first.refreshToken)
        }
    }

    @Test
    fun `logout revokes the current family`() {
        val tutor = savedTutor()
        val first = service.authenticate(LoginCommand(tutor.email, "secret123"))

        service.logout(first.refreshToken)

        assertThrows(InvalidCredentialsException::class.java) {
            service.refresh(first.refreshToken)
        }
    }

    @Test
    fun `logoutAll revokes every session for the user`() {
        val tutor = savedTutor()
        val first = service.authenticate(LoginCommand(tutor.email, "secret123"))
        val second = service.authenticate(LoginCommand(tutor.email, "secret123"))

        service.logoutAll(tutor.id!!)

        assertThrows(InvalidCredentialsException::class.java) { service.refresh(first.refreshToken) }
        assertThrows(InvalidCredentialsException::class.java) { service.refresh(second.refreshToken) }
    }

    private fun sha256(s: String): String {
        val dig = java.security.MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
        return buildString(dig.size * 2) { dig.forEach { append("%02x".format(it)) } }
    }
}
