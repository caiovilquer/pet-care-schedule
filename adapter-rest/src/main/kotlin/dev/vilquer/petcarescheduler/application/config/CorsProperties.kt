package dev.vilquer.petcarescheduler.application.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.cors")
data class CorsProperties(
    var allowedOrigins: List<String> = listOf("http://localhost:4200"),
)
