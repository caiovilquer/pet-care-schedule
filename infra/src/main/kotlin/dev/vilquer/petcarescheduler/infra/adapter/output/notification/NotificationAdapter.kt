package dev.vilquer.petcarescheduler.infra.adapter.output.notification

import dev.vilquer.petcarescheduler.core.domain.entity.Event
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.NotificationPort
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
class EmailNotificationAdapter(
    private val mailSender: JavaMailSender
) : NotificationPort {

    private val log = LoggerFactory.getLogger(EmailNotificationAdapter::class.java)

    override fun sendEventReminder(event: Event) {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message)
        helper.setTo("test@example.com")
        helper.setSubject("PetCareScheduler reminder")
        helper.setText("Event ${'$'}{event.type} on ${'$'}{event.dateStart}: ${'$'}{event.description}")
        mailSender.send(message)
        log.info("Sent fake mail for event {}", event.id)
    }
}