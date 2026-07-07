package dev.vilquer.petcarescheduler.application.adapter.input.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

@ConfigurationProperties("app.security.refresh")
data class RefreshProperties(
    val ttl: Duration = Duration.ofDays(30),
    val cookie: RefreshCookieProperties = RefreshCookieProperties()
)

data class RefreshCookieProperties(
    val name: String = "refresh_token",
    val domain: String? = null,
    val path: String = "/api/v1/auth",
    val sameSite: String = "Lax",
    val secure: Boolean = true
)

@Configuration
@EnableConfigurationProperties(RefreshProperties::class)
open class RefreshPropertiesConfig
