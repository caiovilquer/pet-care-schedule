package dev.vilquer.petcarescheduler.infra.adapter.output.mail

import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.infra.config.MailApiProps
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PasswordResetNotifierPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration

@Component
class MailSenderAdapter(
    @param:Qualifier("mailerSendClient") private val http: RestClient,
    private val props: MailApiProps,
    @param:Value("\${app.frontend.base-url:https://rotinapet.vilquer.dev}") private val frontBase: String,
) : PasswordResetNotifierPort {

    private val log = LoggerFactory.getLogger(MailSenderAdapter::class.java)

    override fun sendResetLink(to: Email, tokenPlain: String, ttl: Duration) {
        val link = "$frontBase/auth/reset-password?token=$tokenPlain"
        val ttlMinutes = ttl.toMinutes()
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

        val payload = mapOf(
            "from" to mapOf("email" to props.from, "name" to props.fromName),
            "to" to listOf(mapOf("email" to to.value)),
            "subject" to subject,
            "text" to text,
            "html" to html
        )

        try {
            val status = http.post()
                .uri("/email")
                .body(payload)
                .retrieve()
                .onStatus({ s -> s.value() >= 400 }) { _, resp ->
                    val body = resp.body.bufferedReader().use { it.readText() }
                    throw IllegalStateException("MailerSend error ${resp.statusCode.value()}: $body")
                }
                .toBodilessEntity()
                .statusCode

            if (status.is2xxSuccessful || status == HttpStatus.ACCEPTED) {
                log.info("Password reset email sent to {}", to.value)
            } else {
                log.error("MailerSend returned status {} sending reset email to {}", status.value(), to.value)
            }
        } catch (ex: Exception) {
            // Uma falha aqui não pode vazar para o request HTTP que disparou o
            // reset: sem este catch, uma exceção do WebClient escapava até o
            // dispatcher, que a redespachava para /error — fora da lista
            // permitAll — e o cliente via um 401 sem relação nenhuma com o que
            // de fato deu errado (confirmado batendo /forgot com a API de
            // e-mail indisponível no smoke test).
            log.error("Failed to call MailerSend API sending reset email to {}", to.value, ex)
        }
    }
}
