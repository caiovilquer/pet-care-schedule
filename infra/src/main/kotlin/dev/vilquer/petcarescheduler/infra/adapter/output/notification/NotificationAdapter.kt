package dev.vilquer.petcarescheduler.infra.adapter.output.notification

import dev.vilquer.petcarescheduler.core.domain.entity.Event
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.NotificationPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PetRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

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

        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message)
        helper.setFrom(from)
        helper.setTo(tutorEmail)
        helper.setSubject("PetCareScheduler reminder")
        helper.setText(renderTemplate(event))
        try {
            mailSender.send(message)
            log.info("Sent mail for event {} to {}", event.id, tutorEmail)
        } catch (ex: MailException) {
            log.error("Failed to send mail for event {}", event.id, ex)
        }
    }

    private fun renderTemplate(event: Event) =
        "Event ${event.type} on ${event.dateStart}: ${event.description}"
}

