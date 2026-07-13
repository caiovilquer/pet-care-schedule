package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.FakePasswordHash
import dev.vilquer.petcarescheduler.application.FakePasswordResetNotifier
import dev.vilquer.petcarescheduler.application.FakeTransactionPort
import dev.vilquer.petcarescheduler.application.InMemoryPasswordResetTokenPort
import dev.vilquer.petcarescheduler.application.InMemoryTutorRepo
import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PasswordResetServiceTest {

    private val tutorRepo = InMemoryTutorRepo()
    private val tokens = InMemoryPasswordResetTokenPort()
    private val notifier = FakePasswordResetNotifier()
    private val passwordHash = FakePasswordHash()
    private val transactionPort = FakeTransactionPort()
    private val service = PasswordResetService(tutorRepo, tokens, notifier, passwordHash, transactionPort)

    private fun savedTutor() = tutorRepo.save(
        Tutor(
            firstName = "Ana",
            lastName = null,
            email = Email.of("ana@example.com").getOrThrow(),
            passwordHash = "old-hash",
            phoneNumber = PhoneNumber.of("+5511912345678").getOrNull()
        )
    )

    @Test
    fun `requestReset creates a token and sends the link`() {
        val tutor = savedTutor()

        service.requestReset(tutor.email, "/invite?token=household-token")

        assertEquals(1, tokens.allTokens().size)
        assertEquals(1, notifier.sent.size)
        assertEquals(tutor.email, notifier.sent.first().to)
        assertEquals("/invite?token=household-token", notifier.sent.first().returnUrl)
    }

    @Test
    fun `requestReset rejects an external return URL`() {
        val tutor = savedTutor()

        service.requestReset(tutor.email, "//example.com/invite")

        assertEquals(null, notifier.sent.single().returnUrl)
    }

    @Test
    fun `requestReset invalidates previously issued tokens`() {
        val tutor = savedTutor()

        service.requestReset(tutor.email)
        val firstToken = notifier.sent.first().tokenPlain
        service.requestReset(tutor.email)

        assertTrue(tokens.allTokens().first { it.tokenHash == sha256(firstToken) }.usedAt != null)
        assertEquals(false, service.validate(firstToken))
    }

    @Test
    fun `requestReset for unknown email is a silent no-op`() {
        service.requestReset(Email.of("ghost@example.com").getOrThrow())

        assertEquals(0, tokens.allTokens().size)
        assertEquals(0, notifier.sent.size)
    }

    @Test
    fun `reset changes the password and marks the token used`() {
        val tutor = savedTutor()
        service.requestReset(tutor.email)
        val tokenPlain = notifier.sent.first().tokenPlain

        service.reset(tokenPlain, "new-password")

        val updated = tutorRepo.findById(tutor.id!!)!!
        assertNotEquals(tutor.passwordHash, updated.passwordHash)
        assertEquals(passwordHash.hash("new-password"), updated.passwordHash)
        assertEquals(false, service.validate(tokenPlain))
    }

    @Test
    fun `reset rejects an unknown token`() {
        assertThrows(IllegalArgumentException::class.java) { service.reset("not-a-real-token", "new-password") }
    }

    @Test
    fun `reset bumps passwordChangedAt so previously issued JWTs are invalidated`() {
        val tutor = savedTutor()
        service.requestReset(tutor.email)
        val tokenPlain = notifier.sent.first().tokenPlain

        service.reset(tokenPlain, "new-password")

        assertNotEquals(tutor.passwordChangedAt, tutorRepo.findById(tutor.id!!)?.passwordChangedAt)
    }

    @Test
    fun `reset rejects passwords outside the supported length`() {
        assertThrows(IllegalArgumentException::class.java) {
            service.reset("any-token", "short")
        }
    }

    private fun sha256(s: String): String {
        val dig = java.security.MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
        return buildString(dig.size * 2) { dig.forEach { append("%02x".format(it.toInt() and 0xff)) } }
    }
}
