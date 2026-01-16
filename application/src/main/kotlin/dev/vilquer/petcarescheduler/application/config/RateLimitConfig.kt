package dev.vilquer.petcarescheduler.application.config

import dev.vilquer.petcarescheduler.application.security.JwtCacheProperties
import dev.vilquer.petcarescheduler.application.service.RateLimitProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(RateLimitProperties::class, JwtCacheProperties::class)
class RateLimitConfig
