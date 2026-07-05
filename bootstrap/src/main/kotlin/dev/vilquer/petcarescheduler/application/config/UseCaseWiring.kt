package dev.vilquer.petcarescheduler.application.config

import dev.vilquer.petcarescheduler.application.service.AuthAppService
import dev.vilquer.petcarescheduler.application.service.EventAppService
import dev.vilquer.petcarescheduler.application.service.PasswordResetService
import dev.vilquer.petcarescheduler.application.service.PetAppService
import dev.vilquer.petcarescheduler.application.service.RateLimitProperties
import dev.vilquer.petcarescheduler.application.service.RateLimiterService
import dev.vilquer.petcarescheduler.application.service.SecurityMaintenanceService
import dev.vilquer.petcarescheduler.application.service.TutorAppService
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.ClockPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.EventRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.NotificationPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PasswordHashPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PasswordResetNotifierPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PasswordResetTokenPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PetRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.RateLimitStorePort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TokenIssuerPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

/**
 * O módulo `application` é Kotlin puro (sem Spring): os services não carregam
 * `@Service`/`@Value`/`@ConfigurationProperties`. Este é o único lugar onde o
 * grafo de use cases é montado — a fronteira hexagonal fica no wiring, não em
 * anotações espalhadas pelos services.
 */
@Configuration
class UseCaseWiring {

    @Bean
    fun authAppService(
        tutorRepo: TutorRepositoryPort,
        passwordHash: PasswordHashPort,
        tokenIssuer: TokenIssuerPort,
    ) = AuthAppService(tutorRepo, passwordHash, tokenIssuer)

    @Bean
    fun eventAppService(
        eventRepo: EventRepositoryPort,
        petRepo: PetRepositoryPort,
        clock: ClockPort,
        notifier: NotificationPort,
    ) = EventAppService(eventRepo, petRepo, clock, notifier)

    @Bean
    fun petAppService(
        petRepo: PetRepositoryPort,
        tutorRepo: TutorRepositoryPort,
        eventRepo: EventRepositoryPort,
    ) = PetAppService(petRepo, tutorRepo, eventRepo)

    @Bean
    fun tutorAppService(
        tutorRepo: TutorRepositoryPort,
        passwordHash: PasswordHashPort,
        petRepo: PetRepositoryPort,
    ) = TutorAppService(tutorRepo, passwordHash, petRepo)

    @Bean
    fun rateLimitProperties(environment: Environment): RateLimitProperties =
        Binder.get(environment)
            .bind("app.security.rate-limit", RateLimitProperties::class.java)
            .orElseGet { RateLimitProperties() }

    @Bean
    fun rateLimiterService(
        props: RateLimitProperties,
        store: RateLimitStorePort,
    ) = RateLimiterService(props, store)

    @Bean
    fun passwordResetService(
        tutors: TutorRepositoryPort,
        tokens: PasswordResetTokenPort,
        notifier: PasswordResetNotifierPort,
        passwordHash: PasswordHashPort,
        environment: Environment,
    ) = PasswordResetService(
        tutors, tokens, notifier, passwordHash,
        ttlMinutes = environment.getProperty("app.reset.ttl-minutes", Long::class.java, 30L),
    )

    @Bean
    fun securityMaintenanceService(
        rateLimiter: RateLimiterService,
        resetTokens: PasswordResetTokenPort,
    ) = SecurityMaintenanceService(rateLimiter, resetTokens)
}
