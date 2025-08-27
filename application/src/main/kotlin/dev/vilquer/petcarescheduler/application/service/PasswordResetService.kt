package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.core.domain.reset.PasswordResetToken
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.MailSenderPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PasswordResetTokenPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.PasswordResetUseCase
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64

@Service
class PasswordResetService(
    private val tutors: TutorRepositoryPort,
    private val tokens: PasswordResetTokenPort,
    private val mail: MailSenderPort,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock = Clock.systemUTC(),
    @param:Value("\${app.mail.from}") private val from: String,
    @param:Value("\${app.frontend.base-url:https://petcare.vilquer.dev}") private val frontBase: String,
    @param:Value("\${app.reset.ttl-minutes:30}") private val ttlMinutes: Long,
) : PasswordResetUseCase {

    override fun requestReset(email: Email) {
        val tutor = tutors.findByEmail(email) ?: return // resposta neutra
        val tokenPlain = generateToken()
        val tokenHash = sha256Hex(tokenPlain)
        val expires = Instant.now(clock).plus(Duration.ofMinutes(ttlMinutes))

        tokens.create(
            PasswordResetToken(
                tokenHash = tokenHash,
                userId = tutor.id ?: throw IllegalStateException("User not found"),
                expiresAt = expires
            )
        )

        sendResetEmail(tutor.email.value, tokenPlain)
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
        tutors.updatePassword(tutor.id!!, passwordEncoder.encode(newPassword))
        tutors.bumpPasswordChangedAt(tutor.id!!, now)
        tokens.markUsed(t.id, now)
    }

    private fun sendResetEmail(to: String, token: String) {
        val link = "$frontBase/auth/reset-password?token=$token"
        val subject = "Redefinição de senha"
        val text = "Use este link para redefinir sua senha: $link (expira em $ttlMinutes minutos)."
        val html = """
          <html><body style="font-family:Arial,Helvetica,sans-serif">
            <h2>Redefinição de senha</h2>
            <p>Para criar uma nova senha, clique no botão abaixo:</p>
            <p><a href="$link" style="background:#2f855a;color:#fff;padding:10px 14px;border-radius:6px;text-decoration:none">Redefinir senha</a></p>
            <p style="color:#666">O link expira em $ttlMinutes minutos.</p>
            <p style="color:#777;font-size:12px">Se você não solicitou, ignore este e-mail.</p>
          </body></html>
        """.trimIndent()

        mail.sendHtml(from, to, subject, text, html)
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