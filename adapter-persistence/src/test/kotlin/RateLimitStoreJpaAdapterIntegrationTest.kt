package petcarescheduler.infra.test

import dev.vilquer.petcarescheduler.infra.AbstractPostgresIntegrationTest
import dev.vilquer.petcarescheduler.infra.PersistenceTestApplication
import dev.vilquer.petcarescheduler.infra.adapter.output.external.RateLimitStoreJpaAdapter
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.RateLimitAttemptRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = [PersistenceTestApplication::class])
@Import(RateLimitStoreJpaAdapter::class)
class RateLimitStoreJpaAdapterIntegrationTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var store: RateLimitStoreJpaAdapter

    @Autowired
    private lateinit var repo: RateLimitAttemptRepository

    @Test
    fun `concurrent first insert accumulates count without integrity errors`() {
        val key = "LOGIN:login:127.0.0.1:concurrent@example.com"
        val now = Instant.parse("2026-01-01T12:00:00Z")
        val window = Duration.ofMinutes(15)
        val threads = 20
        val pool = Executors.newFixedThreadPool(threads)
        try {
            val counts = (1..threads).map {
                CompletableFuture.supplyAsync({ store.registerAttempt(key, now, window) }, pool)
            }.map { it.get() }

            assertEquals(threads, counts.max())
            assertEquals(threads, repo.findById(key).orElseThrow().count)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `window reset uses managed entity under concurrency`() {
        val key = "LOGIN:login:127.0.0.1:window@example.com"
        val window = Duration.ofMinutes(15)
        val expiredStart = Instant.parse("2026-01-01T10:00:00Z")
        val resetAt = expiredStart.plus(window).plusSeconds(1)

        store.registerAttempt(key, expiredStart, window)
        store.registerAttempt(key, expiredStart, window)
        assertEquals(2, repo.findById(key).orElseThrow().count)

        val pool = Executors.newFixedThreadPool(10)
        try {
            val counts = (1..10).map {
                CompletableFuture.supplyAsync({ store.registerAttempt(key, resetAt, window) }, pool)
            }.map { it.get() }

            assertEquals(10, counts.max())
            val saved = repo.findById(key).orElseThrow()
            assertEquals(10, saved.count)
            assertEquals(resetAt, saved.windowStart)
        } finally {
            pool.shutdownNow()
        }
    }
}
