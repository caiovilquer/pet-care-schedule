package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PasswordResetTokenPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.SecurityMaintenanceUseCase
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
class SecurityMaintenanceService(
    private val rateLimiter: RateLimiterService,
    private val resetTokens: PasswordResetTokenPort,
    private val clock: Clock = Clock.systemUTC()
) : SecurityMaintenanceUseCase {
    override fun cleanupSecurityArtifacts() {
        rateLimiter.cleanup()
        resetTokens.cleanup(Instant.now(clock))
    }
}
