package dev.vilquer.petcarescheduler.application.adapter.input.scheduler

import dev.vilquer.petcarescheduler.application.service.RateLimiterService
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PasswordResetTokenPort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
class SecurityMaintenanceScheduler(
    private val rateLimiter: RateLimiterService,
    private val resetTokens: PasswordResetTokenPort,
    private val clock: Clock = Clock.systemUTC()
) {
    @Scheduled(cron = "0 30 3 * * *", zone = "\${app.timezone:America/Sao_Paulo}")
    fun cleanupSecurityArtifacts() {
        rateLimiter.cleanup()
        resetTokens.cleanup(Instant.now(clock))
    }
}
