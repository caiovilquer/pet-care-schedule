package dev.vilquer.petcarescheduler.infra.adapter.output.mail

import dev.vilquer.petcarescheduler.core.domain.household.HouseholdRole
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

    override fun sendInvitation(
        email: String,
        householdName: String,
        inviterName: String,
        role: HouseholdRole,
        token: String,
        expiresAt: Instant,
    ) {
        val link = "$frontBase/invite?token=${URLEncoder.encode(token, StandardCharsets.UTF_8)}"
        val safeHousehold = RotinaPetEmail.escape(householdName)
        val safeInviter = RotinaPetEmail.escape(inviterName)
        val roleLabel = when (role) {
            HouseholdRole.OWNER -> "proprietário"
            HouseholdRole.CAREGIVER -> "cuidador"
            HouseholdRole.VIEWER -> "visualizador"
        }
        val roleDescription = when (role) {
            HouseholdRole.OWNER -> "acesso administrativo completo"
            HouseholdRole.CAREGIVER -> "pode registrar e confirmar cuidados"
            HouseholdRole.VIEWER -> "pode acompanhar a rotina dos pets"
        }
        val content = buildString {
            append(RotinaPetEmail.badge("Convite de família", RotinaPetEmail.GREEN, "#E3F0E7"))
            append(RotinaPetEmail.title("Você foi convidado para cuidar junto"))
            append(
                RotinaPetEmail.paragraph(
                    "<strong>$safeInviter</strong> convidou você para participar da família " +
                        "<strong>$safeHousehold</strong> no RotinaPet e dividir o dia a dia de cuidado com os pets."
                )
            )
            append(
                RotinaPetEmail.detailCard(
                    listOf(
                        "Família" to safeHousehold,
                        "Convidado por" to safeInviter,
                        "Seu papel" to "${roleLabel.replaceFirstChar { it.titlecase() }} — $roleDescription",
                    )
                )
            )
            if (role == HouseholdRole.OWNER) {
                append(
                    RotinaPetEmail.notice(
                        "<strong>Atenção:</strong> como proprietário, você terá acesso administrativo completo a pets, planos, finanças e membros desta família.",
                        RotinaPetEmail.WARNING,
                        RotinaPetEmail.WARNING_BG,
                    )
                )
            }
            append(RotinaPetEmail.ctaButton("Aceitar convite", link))
            append(RotinaPetEmail.fallbackLink(link))
            append(RotinaPetEmail.divider())
            append(
                RotinaPetEmail.mutedNote(
                    "O convite é pessoal, só funciona para este e-mail e expira em <strong>7 dias</strong>. " +
                        "Se você não conhece $safeInviter, pode ignorar esta mensagem."
                )
            )
        }
        val html = RotinaPetEmail.render(
            docTitle = "Convite para a família $safeHousehold",
            preheader = "$safeInviter convidou você para a família $safeHousehold — seu papel será $roleLabel.",
            contentHtml = content,
            footerReason = "Você recebeu este e-mail porque alguém convidou este endereço para uma família no RotinaPet.",
            baseUrl = frontBase,
        )
        val text = """
            Convite de família — RotinaPet

            $inviterName convidou você para a família "$householdName" no RotinaPet.
            Seu papel será: $roleLabel ($roleDescription).

            Aceite o convite em: $link

            O convite é pessoal, só funciona para este e-mail e expira em 7 dias.
        """.trimIndent()
        val payload = mapOf(
            "from" to mapOf("email" to props.from, "name" to props.fromName),
            "to" to listOf(mapOf("email" to email)),
            "subject" to "$inviterName convidou você para cuidar junto",
            "text" to text,
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
}
