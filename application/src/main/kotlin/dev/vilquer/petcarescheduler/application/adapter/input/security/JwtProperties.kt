package dev.vilquer.petcarescheduler.application.adapter.input.security

import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import javax.crypto.SecretKey


@ConfigurationProperties("jwt")
data class JwtProperties(
    val issuer: String,
    val secret: String,
    val expiration: Duration = Duration.ofMinutes(20)
)

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
open class JwtBeans(private val props: JwtProperties) {

    @Bean
    open fun signingKey(): SecretKey {
        return Keys.hmacShaKeyFor(props.secret.toByteArray(Charsets.UTF_8))
    }

    @Bean
    open fun jwtParser(signingKey: SecretKey): JwtParser {
        return Jwts.parser()
            .requireIssuer(props.issuer)
            .verifyWith(signingKey)
            .build()
    }
}