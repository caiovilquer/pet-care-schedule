package dev.vilquer.petcarescheduler.application.config

import dev.vilquer.petcarescheduler.application.security.JwtCacheProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

// RateLimitProperties é vinculado manualmente em UseCaseWiring (bootstrap), já
// que o módulo application não carrega mais anotações Spring.
@Configuration
@EnableConfigurationProperties(JwtCacheProperties::class)
class JwtCacheConfig
