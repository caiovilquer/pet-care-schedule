package dev.vilquer.petcarescheduler.application.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@ConfigurationProperties("app.security.jwt-cache")
data class JwtCacheProperties(
    val ttl: Duration = Duration.ofMinutes(5),
    val maxSize: Int = 10_000,
    val invalidationSkew: Duration = Duration.ofSeconds(5)
)

@Component
class PasswordChangedAtCache(
    private val props: JwtCacheProperties,
    private val clock: Clock = Clock.systemUTC()
) {
    private data class Entry(val value: Instant?, val expiresAt: Instant)

    private val cache = ConcurrentHashMap<Long, Entry>()

    fun getOrLoad(tutorId: Long, loader: () -> Instant?): Instant? {
        val now = clock.instant()
        val existing = cache[tutorId]
        if (existing != null && now.isBefore(existing.expiresAt)) {
            return existing.value
        }
        val loaded = loader()
        if (cache.size >= props.maxSize) {
            cache.clear()
        }
        cache[tutorId] = Entry(loaded, now.plus(props.ttl))
        return loaded
    }
}
