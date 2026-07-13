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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Duration

@Component
class MailSenderAdapter(
    @param:Qualifier("mailerSendClient") private val http: RestClient,
    private val props: MailApiProps,
    @param:Value("\${app.frontend.base-url:https://rotinapet.vilquer.dev}") private val frontBase: String,
) : PasswordResetNotifierPort {

    private val log = LoggerFactory.getLogger(MailSenderAdapter::class.java)

    override fun sendResetLink(to: Email, tokenPlain: String, ttl: Duration, returnUrl: String?) {
        val returnQuery = returnUrl?.let { "&returnUrl=${URLEncoder.encode(it, StandardCharsets.UTF_8)}" }.orEmpty()
        val link = "$frontBase/auth/reset-password?token=$tokenPlain$returnQuery"
        val ttlMinutes = ttl.toMinutes()
        val subject = "Redefinição de senha"
        val text = """
            Redefinição de senha — RotinaPet

            Recebemos um pedido para redefinir a senha da sua conta.
            Para criar uma nova senha, acesse: $link

            O link expira em $ttlMinutes minutos e só pode ser usado uma vez.
            Se você não pediu a redefinição, ignore este e-mail — sua senha continua a mesma.
        """.trimIndent()
        val content = buildString {
            append(RotinaPetEmail.title("Vamos criar uma nova senha?"))
            append(
                RotinaPetEmail.paragraph(
                    "Recebemos um pedido para redefinir a senha da conta " +
                        "<strong>${RotinaPetEmail.escape(to.value)}</strong> no RotinaPet. " +
                        "É só usar o botão abaixo para escolher uma nova senha."
                )
            )
            append(RotinaPetEmail.ctaButton("Redefinir senha", link))
            append(RotinaPetEmail.fallbackLink(link))
            append(RotinaPetEmail.divider())
            append(
                RotinaPetEmail.mutedNote(
                    "O link expira em <strong>$ttlMinutes minutos</strong> e só pode ser usado uma vez. " +
                        "Se você não pediu a redefinição, pode ignorar este e-mail — sua senha continua a mesma."
                )
            )
        }
        val html = RotinaPetEmail.render(
            docTitle = subject,
            preheader = "Crie uma nova senha para a sua conta — o link expira em $ttlMinutes minutos.",
            contentHtml = content,
            footerReason = "Você recebeu este e-mail porque uma redefinição de senha foi solicitada para este endereço no RotinaPet.",
            baseUrl = frontBase,
        )

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
