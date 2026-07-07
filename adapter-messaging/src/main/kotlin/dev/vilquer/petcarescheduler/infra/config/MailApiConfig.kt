package dev.vilquer.petcarescheduler.infra.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@ConfigurationProperties(prefix = "app.mail")
data class MailApiProps(
    var apiKey: String = "",
    var from: String = "",
    var fromName: String = "RotinaPet",
    var baseUrl: String = ""
)

@Configuration
@EnableConfigurationProperties(MailApiProps::class)
class MailApiConfig {
    // RestClient síncrono sobre o HttpClient da JDK: as duas chamadas ao
    // MailerSend sempre foram bloqueantes (.block()), então o stack
    // WebFlux/Netty/Reactor inteiro ficava carregado no processo só para
    // isso — dezenas de MB de metaspace/threads em idle.
    @Bean
    fun mailerSendClient(props: MailApiProps): RestClient {
        val requestFactory = JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
        ).apply { setReadTimeout(Duration.ofSeconds(30)) }

        return RestClient.builder()
            .baseUrl("${props.baseUrl}/v1")
            .requestFactory(requestFactory)
            .defaultHeaders {
                it.setBearerAuth(props.apiKey)
                it.contentType = MediaType.APPLICATION_JSON
                it.accept = listOf(MediaType.APPLICATION_JSON)
            }
            .build()
    }
}
