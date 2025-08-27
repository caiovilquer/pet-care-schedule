package dev.vilquer.petcarescheduler.infra.adapter.output.mail


import dev.vilquer.petcarescheduler.usecase.contract.drivenports.MailSenderPort
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
class MailSenderAdapter(
    private val mailSender: JavaMailSender
) : MailSenderPort {
    override fun sendHtml(from: String, to: String, subject: String, text: String, html: String) {
        val msg = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(msg, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8")
        helper.setFrom(from)
        helper.setTo(to)
        helper.setSubject(subject)
        helper.setText(text, html)
        mailSender.send(msg)
    }
}
