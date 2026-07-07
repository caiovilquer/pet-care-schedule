package dev.vilquer.petcarescheduler.infra.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@ConfigurationProperties(prefix = "app.google")
data class GoogleApiProps(
    var apiKey: String = "",
    var language: String = "pt-BR",
    var region: String = "BR"
)

@Configuration
@EnableConfigurationProperties(GoogleApiProps::class)
class GoogleApiConfig {
    // RestClient síncrono sobre o HttpClient da JDK, igual ao MailApiConfig —
    // consistência com o resto do módulo (sem reintroduzir WebFlux/Netty).
    // followRedirects(NORMAL) é necessário para o endpoint de foto, que
    // responde com um 302 apontando para os bytes reais da imagem.
    @Bean
    fun googlePlacesClient(): RestClient {
        val requestFactory = JdkClientHttpRequestFactory(
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
        ).apply { setReadTimeout(Duration.ofSeconds(15)) }

        return RestClient.builder()
            .baseUrl("https://maps.googleapis.com/maps/api")
            .requestFactory(requestFactory)
            .build()
    }
}
