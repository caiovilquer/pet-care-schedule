package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.exception.RateLimitException
import dev.vilquer.petcarescheduler.application.security.RateLimitAttempt
import dev.vilquer.petcarescheduler.application.security.RateLimitAttemptRepository
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant

@ConfigurationProperties("app.security.rate-limit")
data class RateLimitProperties(
    val login: RateLimitConfig = RateLimitConfig(),
    val passwordReset: RateLimitConfig = RateLimitConfig()
)

data class RateLimitConfig(
    val maxAttempts: Int = 5,
    val window: Duration = Duration.ofMinutes(15)
)

enum class RateLimitAction { LOGIN, PASSWORD_RESET }

@Service
class RateLimiterService(
    private val props: RateLimitProperties,
    private val repo: RateLimitAttemptRepository,
    private val clock: Clock = Clock.systemUTC()
) {
    @Transactional
    fun check(action: RateLimitAction, key: String) {
        val limit = when (action) {
            RateLimitAction.LOGIN -> props.login
            RateLimitAction.PASSWORD_RESET -> props.passwordReset
        }
        val now = clock.instant()
        val id = "${action.name}:${key}"

        var attempt = 0
        while (true) {
            try {
                val existing = repo.findById(id).orElse(null)
                val updated = if (existing == null || now.isAfter(existing.windowStart.plus(limit.window))) {
                    RateLimitAttempt(id = id, count = 1, windowStart = now)
                } else {
                    existing.count += 1
                    existing
                }
                repo.save(updated)
                if (updated.count > limit.maxAttempts) {
                    throw RateLimitException("Limite de tentativas excedido, tente novamente mais tarde")
                }
                return
            } catch (ex: OptimisticLockingFailureException) {
                attempt++
                if (attempt >= 3) throw ex
            }
        }
    }

    @Transactional
    fun cleanup() {
        val maxWindow = maxOf(props.login.window, props.passwordReset.window)
        val cutoff = clock.instant().minus(maxWindow)
        repo.deleteOlderThan(cutoff)
    }
}
