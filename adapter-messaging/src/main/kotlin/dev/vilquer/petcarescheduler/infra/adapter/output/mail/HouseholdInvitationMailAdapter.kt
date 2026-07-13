package dev.vilquer.petcarescheduler.infra.adapter.output.mail

import dev.vilquer.petcarescheduler.infra.config.MailApiProps
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.HouseholdInvitationNotifierPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant

@Component
class HouseholdInvitationMailAdapter(
    @param:Qualifier("mailerSendClient") private val http: RestClient,
    private val props: MailApiProps,
    @param:Value("\${app.frontend.base-url:https://rotinapet.vilquer.dev}") private val frontBase: String,
) : HouseholdInvitationNotifierPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendInvitation(email: String, householdName: String, inviterName: String, token: String, expiresAt: Instant) {
        val link = "$frontBase/invite?token=${URLEncoder.encode(token, StandardCharsets.UTF_8)}"
        val safeHousehold = escape(householdName)
        val safeInviter = escape(inviterName)
        val html = """
            <html lang="pt-BR"><body style="font-family:Arial,sans-serif;color:#24332b">
              <h2>Você foi convidado para cuidar junto</h2>
              <p><strong>$safeInviter</strong> convidou você para a família <strong>$safeHousehold</strong> no RotinaPet.</p>
              <p><a href="$link" style="display:inline-block;background:#146c43;color:#fff;padding:12px 18px;border-radius:10px;text-decoration:none">Aceitar convite</a></p>
              <p style="color:#66756d;font-size:13px">O convite é pessoal, só funciona para este e-mail e expira em 7 dias.</p>
            </body></html>
        """.trimIndent()
        val payload = mapOf(
            "from" to mapOf("email" to props.from, "name" to props.fromName),
            "to" to listOf(mapOf("email" to email)),
            "subject" to "$inviterName convidou você para cuidar junto",
            "text" to "$inviterName convidou você para a família $householdName no RotinaPet. Aceite em: $link",
            "html" to html,
        )
        try {
            http.post().uri("/email").body(payload).retrieve()
                .onStatus({ it.value() >= 400 }) { _, response ->
                    throw IllegalStateException("Mail API respondeu ${response.statusCode.value()}")
                }.toBodilessEntity()
        } catch (ex: Exception) {
            log.error("Falha ao enviar convite de família para {}", email, ex)
            throw ex
        }
    }

    private fun escape(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
