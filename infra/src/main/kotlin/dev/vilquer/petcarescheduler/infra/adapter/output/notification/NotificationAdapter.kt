package dev.vilquer.petcarescheduler.infra.adapter.output.notification

import dev.vilquer.petcarescheduler.core.domain.entity.Event
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.NotificationPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PetRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
class EmailNotificationAdapter(
    private val mailSender: JavaMailSender,
    private val tutorRepo: TutorRepositoryPort,
    private val petRepo: PetRepositoryPort,
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
        helper.setTo(tutorEmail)
        helper.setSubject("PetCareScheduler reminder")
        helper.setText(renderTemplate(event))
        mailSender.send(message)
        log.info("Sent fake mail for event {}", event.id)
    }

    private fun renderTemplate(event: Event) =
        "Event ${event.type} on ${event.dateStart}: ${event.description}"
}

