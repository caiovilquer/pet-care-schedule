package dev.vilquer.petcarescheduler.infra.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@ConfigurationProperties(prefix = "app.mail")
data class MailApiProps(
    var apiKey: String = "",
    var from: String = "",
    var fromName: String = "PetCare Scheduler",
    var baseUrl: String = ""
)

@Configuration
@EnableConfigurationProperties(MailApiProps::class)
class MailApiConfig {
    @Bean
    fun mailerSendClient(props: MailApiProps): WebClient =
        WebClient.builder()
            .baseUrl("${props.baseUrl}/v1")
            .defaultHeaders {
                it.setBearerAuth(props.apiKey)
                it.contentType = MediaType.APPLICATION_JSON
                it.accept = listOf(MediaType.APPLICATION_JSON)
            }
            .build()
}
