package dev.vilquer.petcarescheduler.infra.adapter.output.notification


import dev.vilquer.petcarescheduler.core.domain.entity.Event
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.EventReminderTarget
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.CareReminderNotificationTarget
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.CareEscalationNotificationTarget
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.NotificationPort
import dev.vilquer.petcarescheduler.infra.config.MailApiProps
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.client.RestClient
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Component
class EmailNotificationAdapter(
    // Qualificado explicitamente: desde a adição do googlePlacesClient, há
    // dois beans RestClient no contexto.
    @param:Qualifier("mailerSendClient") private val http: RestClient,
    private val props: MailApiProps,
    @param:Value("\${app.timezone:America/Sao_Paulo}") private val timezone: String,
    @param:Value("\${app.frontend.base-url:https://rotinapet.vilquer.dev}") private val frontendBaseUrl: String,
) : NotificationPort {

    private val log = LoggerFactory.getLogger(EmailNotificationAdapter::class.java)

    override fun sendEventReminder(target: EventReminderTarget): Boolean {
        val event = target.event
        val html = renderTemplate(event, target.petName)
        val subject = "Lembrete do RotinaPet: ${event.type.pt()}"

        val payload = mapOf(
            "from" to mapOf("email" to props.from, "name" to props.fromName),
            "to" to listOf(mapOf("email" to target.tutorEmail)),
            "subject" to subject,
            "text" to stripHtml(html),
            "html" to html
        )

        return try {
            val status =
                http.post().uri("/email").body(payload).retrieve().onStatus({ s -> s.value() >= 400 }) { _, resp ->
                    val body = resp.body.bufferedReader().use { it.readText() }
                    throw IllegalStateException("Mail Send error ${resp.statusCode.value()}: $body")
                }.toBodilessEntity().statusCode

            val ok = status.is2xxSuccessful || status == HttpStatus.ACCEPTED
            if (ok) {
                log.info("Sent mail (API) for event {} to {}", event.id, target.tutorEmail)
            } else {
                log.error("Mail Send returned status {} for event {}", status.value(), event.id)
            }
            ok
        } catch (ex: Exception) {
            log.error("Failed to call Mail Send API for event {}", event.id, ex)
            false
        }
    }

    override fun sendCareReminder(target: CareReminderNotificationTarget): Boolean {
        val html = renderTemplate(target.type, target.title, target.dueAt, target.petName)
        val payload = mapOf(
            "from" to mapOf("email" to props.from, "name" to props.fromName),
            "to" to listOf(mapOf("email" to target.tutorEmail)),
            "subject" to "Lembrete do RotinaPet: ${target.type.pt()}",
            "text" to stripHtml(html),
            "html" to html,
        )
        return try {
            val status = http.post().uri("/email").body(payload).retrieve()
                .onStatus({ it.value() >= 400 }) { _, response ->
                    val body = response.body.bufferedReader().use { it.readText() }
                    throw IllegalStateException("Mail Send error ${response.statusCode.value()}: $body")
                }.toBodilessEntity().statusCode
            status.is2xxSuccessful || status == HttpStatus.ACCEPTED
        } catch (ex: Exception) {
            log.error("Failed to send care reminder for occurrence {}", target.occurrenceId.value, ex)
            false
        }
    }

    override fun sendCareEscalation(target: CareEscalationNotificationTarget): Boolean {
        val whenText = target.dueAt.atZone(DEFAULTZONE).format(DATEFMT).replaceFirstChar { it.titlecase(PTBR) }
        val html = """
            <html lang="pt-BR"><body style="font-family:Arial,sans-serif;color:#26342d">
              <h2 style="color:#9b2c2c">Cuidado crítico ainda não confirmado</h2>
              <p>O cuidado <strong>${escapeHtml(target.careTitle)}</strong> de <strong>${escapeHtml(target.petName)}</strong>, previsto para $whenText, continua pendente.</p>
              <p><a href="${frontendBaseUrl.trimEnd('/')}/today" style="display:inline-block;background:#9b2c2c;color:#fff;padding:12px 18px;border-radius:8px;text-decoration:none">Ver cuidado agora</a></p>
              <p style="color:#66756d;font-size:12px">Este alerta foi configurado por um proprietário da família no RotinaPet.</p>
            </body></html>
        """.trimIndent()
        val payload = mapOf(
            "from" to mapOf("email" to props.from, "name" to props.fromName),
            "to" to listOf(mapOf("email" to target.recipientEmail)),
            "subject" to "Atenção: cuidado crítico de ${target.petName} está pendente",
            "text" to stripHtml(html), "html" to html,
        )
        return try {
            http.post().uri("/email").body(payload).retrieve()
                .onStatus({ it.value() >= 400 }) { _, response ->
                    throw IllegalStateException("Mail Send error ${response.statusCode.value()}")
                }.toBodilessEntity().statusCode.is2xxSuccessful
        } catch (ex: Exception) {
            log.error("Failed to send critical care escalation", ex)
            false
        }
    }

    // ===== apresentação =====

    private val PTBR: Locale = Locale.of("pt", "BR")
    private val DEFAULTZONE = parseZoneId(timezone)
    private val DATEFMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy 'às' HH:mm", PTBR)

    private fun EventType?.pt(): String = when (this) {
        EventType.VACCINE -> "Vacinação"
        EventType.MEDICINE -> "Medicação"
        EventType.DIARY -> "Diário"
        EventType.BREED -> "Reprodução"
        EventType.SERVICE -> "Serviço"
        else -> "Evento"
    }

    private fun renderTemplate(event: Event, petNameRaw: String?, zoneId: ZoneId = DEFAULTZONE): String {
        return renderTemplate(event.type, event.description, event.dateStart, petNameRaw, zoneId)
    }

    private fun renderTemplate(
        type: EventType,
        descriptionRaw: String?,
        dateStart: java.time.LocalDateTime,
        petNameRaw: String?,
        zoneId: ZoneId = DEFAULTZONE,
    ): String {
        val tipo = type.pt()
        val dataStr = dateStart.atZone(zoneId).format(DATEFMT).replaceFirstChar { it.titlecase(PTBR) }

        val petName = petNameRaw?.let { escapeHtml(it) } ?: "Pet"
        val descricao = descriptionRaw?.takeIf { it.isNotBlank() }?.let { escapeHtml(it) } ?: "Sem descrição"
        val ctaUrl = "${frontendBaseUrl.trimEnd('/')}/today"

        return """
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html lang="pt-BR" xmlns="http://www.w3.org/1999/xhtml">
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Lembrete RotinaPet</title>
</head>
<body style="margin: 0; padding: 0; background-color: #f0f0f0;">
  <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%">
    <tr>
      <td style="padding: 20px 0;">
        <table align="center" border="0" cellpadding="0" cellspacing="0" width="560" style="border-collapse: collapse; border: 1px solid #e6e6e6; border-radius: 10px; background-color: #ffffff;">
          <tr>
            <td style="padding: 16px; font-family: Arial, Helvetica, sans-serif;">
              
              <h1 style="font-size: 18px; margin: 0 0 8px 0; font-family: Arial, Helvetica, sans-serif; font-weight: bold; color: #333333;">
                Lembrete: $tipo
              </h1>

              <p style="margin: 0 0 12px 0; font-family: Arial, Helvetica, sans-serif; color: #555555; font-size: 16px;">
                <strong>Pet:</strong> <span style="font-weight: 600;">$petName</span>
              </p>

              <p style="margin: 0 0 12px 0; font-family: Arial, Helvetica, sans-serif; color: #555555; font-size: 16px;">
                <strong>Quando:</strong> $dataStr
              </p>

              <p style="margin: 0; font-family: Arial, Helvetica, sans-serif; color: #333333; font-size: 16px; line-height: 24px;">
                <strong>Detalhes:</strong> $descricao
              </p>
              
              <table border="0" cellpadding="0" cellspacing="0" role="presentation" style="margin: 16px 0;">
                <tr>
                  <td align="left" style="border-radius: 6px;" bgcolor="#0895c4">
                    <a href="$ctaUrl" target="_blank" rel="noopener" style="display: inline-block; padding: 10px 16px; font-family: Arial, Helvetica, sans-serif; font-size: 16px; color: #ffffff; text-decoration: none; border-radius: 6px;">
                      Ver cuidado
                    </a>
                  </td>
                </tr>
              </table>

              <p style="margin: 16px 0 0 0; font-family: Arial, Helvetica, sans-serif; color: #777777; font-size: 12px; line-height: 18px;">
                Você recebeu este lembrete porque cadastrou um plano de cuidado no RotinaPet.
              </p>

            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
        """.trimIndent()
    }

    // ===== util =====

    private fun stripHtml(html: String) =
        html.replace(Regex("<[^>]*>"), " ").replace("&nbsp;", " ").replace(Regex("\\s+"), " ").trim()

    private fun escapeHtml(s: String): String = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun parseZoneId(value: String): ZoneId =
        try {
            ZoneId.of(value)
        } catch (ex: Exception) {
            ZoneId.of("America/Sao_Paulo")
        }
}










