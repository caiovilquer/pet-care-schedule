package dev.vilquer.petcarescheduler.infra.adapter.output.notification


import dev.vilquer.petcarescheduler.core.domain.entity.Event
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.NotificationPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PetRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import dev.vilquer.petcarescheduler.infra.config.MailApiProps
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Component
class EmailNotificationAdapter(
    private val http: WebClient,
    private val props: MailApiProps,
    private val tutorRepo: TutorRepositoryPort,
    private val petRepo: PetRepositoryPort,
) : NotificationPort {

    private val log = LoggerFactory.getLogger(EmailNotificationAdapter::class.java)

    override fun sendEventReminder(event: Event) {
        val tutorEmail =
            event.petId?.let { petRepo.findById(it) }?.tutorId?.let { tutorRepo.findById(it)?.email?.value }

        if (tutorEmail == null) {
            log.warn("Could not determine tutor email for event {}", event.id)
            return
        }

        val html = renderTemplate(event)
        val subject = "Lembrete do PetCare: ${event.type.pt()}"

        val payload = mapOf(
            "from" to mapOf("email" to props.from, "name" to props.fromName),
            "to" to listOf(mapOf("email" to tutorEmail)),
            "subject" to subject,
            // envie um dos dois ou ambos:
            "text" to stripHtml(html),
            "html" to html
        )

        try {
            val status =
                http.post().uri("/email").bodyValue(payload).retrieve().onStatus({ s -> s.value() >= 400 }) { resp ->
                    resp.bodyToMono(String::class.java).map { body ->
                        IllegalStateException("Mail Send error ${resp.statusCode().value()}: $body")
                    }
                }.toBodilessEntity().map { it.statusCode }.block() ?: HttpStatus.ACCEPTED

            if (status.is2xxSuccessful || status == HttpStatus.ACCEPTED) {
                log.info("Sent mail (API) for event {} to {}", event.id, tutorEmail)
            } else {
                log.error("Mail Send returned status {} for event {}", status.value(), event.id)
            }
        } catch (ex: Exception) {
            log.error("Failed to call Mail Send API for event {}", event.id, ex)
        }
    }

    // ===== apresentação =====

    private val PTBR = Locale("pt", "BR")
    private val DEFAULTZONE = ZoneId.of("America/Sao_Paulo")
    private val DATEFMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy 'às' HH:mm", PTBR)

    private fun EventType?.pt(): String = when (this) {
        EventType.VACCINE -> "Vacinação"
        EventType.MEDICINE -> "Medicação"
        EventType.DIARY -> "Diário"
        EventType.BREED -> "Reprodução"
        EventType.SERVICE -> "Serviço"
        else -> "Evento"
    }

    private fun renderTemplate(event: Event, zoneId: ZoneId = DEFAULTZONE): String {
        val tipo = event.type.pt()
        val dataStr = event.dateStart.atZone(zoneId).format(DATEFMT).replaceFirstChar { it.titlecase(PTBR) }

        val petName = event.petId?.let { petRepo.findById(it) }?.name
        val descricao = event.description?.takeIf { it.isNotBlank() } ?: "Sem descrição"
        val ctaUrl = "https://petcare.vilquer.dev/events"

        return """
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html lang="pt-BR" xmlns="http://www.w3.org/1999/xhtml">
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Lembrete PetCare</title>
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
                      Ver evento
                    </a>
                  </td>
                </tr>
              </table>

              <p style="margin: 16px 0 0 0; font-family: Arial, Helvetica, sans-serif; color: #777777; font-size: 12px; line-height: 18px;">
                Você recebeu este lembrete porque cadastrou um evento no PetCare Scheduler.
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
}
















