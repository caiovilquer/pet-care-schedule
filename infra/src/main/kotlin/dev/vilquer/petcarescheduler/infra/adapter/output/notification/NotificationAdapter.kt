package dev.vilquer.petcarescheduler.infra.adapter.output.notification

import dev.vilquer.petcarescheduler.core.domain.entity.Event
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.NotificationPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PetRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
// Se usar imagem inline (logo), descomentar a linha abaixo e o addInline no HTML:
// import org.springframework.core.io.ClassPathResource

@Component
class EmailNotificationAdapter(
    private val mailSender: JavaMailSender,
    private val tutorRepo: TutorRepositoryPort,
    private val petRepo: PetRepositoryPort,
    @param:Value("\${app.mail.from}") private val from: String,
) : NotificationPort {

    private val log = LoggerFactory.getLogger(EmailNotificationAdapter::class.java)

    override fun sendEventReminder(event: Event) {
        val tutorEmail = event.petId
            ?.let { petRepo.findById(it) }
            ?.tutorId
            ?.let { tutorRepo.findById(it)?.email?.value }

        if (tutorEmail == null) {
            log.warn("Could not determine tutor email for event {}", event.id)
            return
        }

// ===== Dados formatados que vão para o e-mail =====
        val tipo = event.type.pt()
        val dataStr = event.dateStart.atZone(DEFAULTZONE)
            .format(DATEFMT)
            .replaceFirstChar { it.titlecase(PTBR) }
        val pet = event.petId?.let { petRepo.findById(it) }
        val petName = pet?.name ?: "seu pet"
        val descricao = event.description?.takeIf { it.isNotBlank() } ?: "Sem descrição"
        val ctaUrl = "https://petcare.vilquer.dev/app/events"

        val plain = """
            $tipo de $petName
            Quando: $dataStr
            Detalhes: $descricao
            Acesse: $ctaUrl
        """.trimIndent()

        val html = """
            <!doctype html>
            <html lang="pt-BR">
              <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width">
                <title>Lembrete PetCare</title>
                <style>
                  .card{max-width:560px;margin:0 auto;font-family:Arial,Helvetica,sans-serif;
                        border:1px solid #e6e6e6;border-radius:10px;padding:16px}
                  .title{font-size:18px;margin:0 0 8px}
                  .meta{color:#555;margin:0 0 12px}
                  .btn{display:inline-block;padding:10px 16px;text-decoration:none;
                       background:#2f855a;color:#fff;border-radius:6px}
                  .muted{color:#777;font-size:12px;margin-top:16px}
                  .pet{font-weight:600}
                </style>
              </head>
              <body>
                <div class="card">
                  <!-- Para logo via CID, descomente o addInline abaixo e use: <img src="cid:logo" alt="PetCare" width="48" height="48" /> -->
                  <h1 class="title">Lembrete: $tipo</h1>
                  <p class="meta"><strong>Pet:</strong> <span class="pet">$petName</span></p>
                  <p class="meta"><strong>Quando:</strong> $dataStr</p>
                  <p><strong>Detalhes:</strong> $descricao</p>
                  <p style="margin:16px 0">
                    <a class="btn" href="$ctaUrl" target="_blank" rel="noopener">Ver evento</a>
                  </p>
                  <p class="muted">
                    Você recebeu este lembrete porque cadastrou um evento no PetCare Scheduler.
                  </p>
                </div>
              </body>
            </html>
        """.trimIndent()

        val msg = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(
            msg,
            MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
            Charsets.UTF_8.name()
        )

        // Se usar logo em resources/static/logo.png:
        // helper.addInline("logo", ClassPathResource("static/logo.png"))

        helper.setFrom(from)
        helper.setTo(tutorEmail)
        helper.setSubject("Lembrete • $tipo")
        helper.setText(plain, html)

        try {
            mailSender.send(msg)
            log.info("Sent mail for event {} to {}", event.id, tutorEmail)
        } catch (ex: MailException) {
            log.error("Failed to send mail for event {}", event.id, ex)
        }
    }

    private val PTBR = Locale("pt", "BR")
    private val DEFAULTZONE = ZoneId.of("America/Sao_Paulo")
    private val DATEFMT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy 'às' HH:mm", PTBR)

    private fun EventType.pt(): String = when (this) {
        EventType.VACCINE  -> "Vacinação"
        EventType.MEDICINE -> "Medicação"
        EventType.DIARY    -> "Diário"
        EventType.BREED    -> "Reprodução"
        EventType.SERVICE  -> "Serviço"
    }
}
