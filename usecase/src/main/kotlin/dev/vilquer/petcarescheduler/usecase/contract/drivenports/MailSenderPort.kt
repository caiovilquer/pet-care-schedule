package dev.vilquer.petcarescheduler.usecase.contract.drivenports

interface MailSenderPort {
    fun sendHtml (from: String, to: String, subject: String, text: String, html: String)
}