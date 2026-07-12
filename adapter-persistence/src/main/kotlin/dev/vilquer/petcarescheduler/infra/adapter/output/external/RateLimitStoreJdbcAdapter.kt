package dev.vilquer.petcarescheduler.infra.adapter.output.external

import dev.vilquer.petcarescheduler.usecase.contract.drivenports.RateLimitStorePort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant

@Repository
class RateLimitStoreJdbcAdapter(private val jdbc: JdbcTemplate) : RateLimitStorePort {
    private val h2Locks = Array(64) { Any() }
    private val isPostgres: Boolean by lazy {
        jdbc.dataSource!!.connection.use { it.metaData.databaseProductName.contains("PostgreSQL", ignoreCase = true) }
    }

    override fun registerAttempt(key: String, now: Instant, window: Duration): Int {
        val cutoff = now.minus(window)
        return if (isPostgres) registerPostgres(key, now, cutoff) else registerH2(key, now, cutoff)
    }

    private fun registerPostgres(key: String, now: Instant, cutoff: Instant): Int =
        requireNotNull(
            jdbc.queryForObject(
                """
                insert into rate_limit_attempt (id, count, window_start, version)
                values (?, 1, ?, 0)
                on conflict (id) do update
                   set count = case
                                   when rate_limit_attempt.window_start < ? then 1
                                   else rate_limit_attempt.count + 1
                               end,
                       window_start = case
                                          when rate_limit_attempt.window_start < ? then excluded.window_start
                                          else rate_limit_attempt.window_start
                                      end,
                       version = rate_limit_attempt.version + 1
                returning count
                """.trimIndent(),
                Int::class.java,
                key,
                Timestamp.from(now),
                Timestamp.from(cutoff),
                Timestamp.from(cutoff),
            ),
        )

    private fun registerH2(key: String, now: Instant, cutoff: Instant): Int = synchronized(
        h2Locks[(key.hashCode() and Int.MAX_VALUE) % h2Locks.size],
    ) {
        val updated = jdbc.update(
            """
            update rate_limit_attempt
               set count = case when window_start < ? then 1 else count + 1 end,
                   window_start = case when window_start < ? then ? else window_start end,
                   version = coalesce(version, 0) + 1
             where id = ?
            """.trimIndent(),
            Timestamp.from(cutoff),
            Timestamp.from(cutoff),
            Timestamp.from(now),
            key,
        )
        if (updated == 0) {
            jdbc.update(
                "insert into rate_limit_attempt (id, count, window_start, version) values (?, 1, ?, 0)",
                key,
                Timestamp.from(now),
            )
        }
        requireNotNull(jdbc.queryForObject("select count from rate_limit_attempt where id = ?", Int::class.java, key))
    }

    override fun delete(key: String) {
        jdbc.update("delete from rate_limit_attempt where id = ?", key)
    }

    override fun deleteOlderThan(cutoff: Instant): Int =
        jdbc.update("delete from rate_limit_attempt where window_start < ?", Timestamp.from(cutoff))
}
