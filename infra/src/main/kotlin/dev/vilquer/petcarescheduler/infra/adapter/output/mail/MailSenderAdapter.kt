package dev.vilquer.petcarescheduler.infra.adapter.output.mail

import dev.vilquer.petcarescheduler.usecase.contract.drivenports.MailSenderPort
import dev.vilquer.petcarescheduler.infra.config.MailApiProps
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class MailSenderAdapter(
    @param:Qualifier("mailerSendClient") private val http: WebClient,
    private val props: MailApiProps
) : MailSenderPort {

    private val log = LoggerFactory.getLogger(MailSenderAdapter::class.java)

    override fun sendHtml(from: String, to: String, subject: String, text: String, html: String) {
        val fromEmail = from.ifBlank { props.from }
        val payload = mapOf(
            "from" to mapOf("email" to fromEmail, "name" to props.fromName),
            "to" to listOf(mapOf("email" to to)),
            "subject" to subject,
            "text" to (text.ifBlank { stripHtml(html) }),
            "html" to html
        )

        val status = http.post()
            .uri("/email")
            .bodyValue(payload)
            .retrieve()
            .onStatus({ s -> s.value() >= 400 }) { resp ->
                resp.bodyToMono(String::class.java).map { body ->
                    IllegalStateException("MailerSend error ${resp.statusCode().value()}: $body")
                }
            }
            .toBodilessEntity()
            .map { it.statusCode }
            .block() ?: HttpStatus.ACCEPTED

        if (status.is2xxSuccessful || status == HttpStatus.ACCEPTED) {
            log.info("Password reset email sent to {}", to)
        } else {
            log.error("MailerSend returned status {} sending reset email to {}", status.value(), to)
        }
    }

    private fun stripHtml(html: String) =
        html.replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}

