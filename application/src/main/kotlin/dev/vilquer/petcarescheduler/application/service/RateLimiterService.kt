package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.exception.RateLimitException
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.RateLimitStorePort
import java.time.Clock
import java.time.Duration
import java.time.Instant

// Binding a partir de application.yml (app.security.rate-limit) é feito no bootstrap.
data class RateLimitProperties(
    val login: RateLimitConfig = RateLimitConfig(),
    val passwordReset: RateLimitConfig = RateLimitConfig()
)

data class RateLimitConfig(
    val maxAttempts: Int = 5,
    val window: Duration = Duration.ofMinutes(15)
)

enum class RateLimitAction { LOGIN, PASSWORD_RESET }

class RateLimiterService(
    private val props: RateLimitProperties,
    private val store: RateLimitStorePort,
    private val clock: Clock = Clock.systemUTC()
) {
    fun check(action: RateLimitAction, key: String) {
        val limit = when (action) {
            RateLimitAction.LOGIN -> props.login
            RateLimitAction.PASSWORD_RESET -> props.passwordReset
        }
        val id = "${action.name}:${key}"
        val count = store.registerAttempt(id, clock.instant(), limit.window)
        if (count > limit.maxAttempts) {
            throw RateLimitException("Limite de tentativas excedido, tente novamente mais tarde")
        }
    }

    fun cleanup() {
        val maxWindow = maxOf(props.login.window, props.passwordReset.window)
        store.deleteOlderThan(clock.instant().minus(maxWindow))
    }
}
