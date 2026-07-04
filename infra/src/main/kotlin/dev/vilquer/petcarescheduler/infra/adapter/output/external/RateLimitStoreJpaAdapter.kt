package dev.vilquer.petcarescheduler.infra.adapter.output.external

import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.RateLimitAttempt
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.RateLimitAttemptRepository
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.RateLimitStorePort
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Repository
class RateLimitStoreJpaAdapter(
    private val repo: RateLimitAttemptRepository
) : RateLimitStorePort {

    @Transactional
    override fun registerAttempt(key: String, now: Instant, window: Duration): Int {
        var attempt = 0
        while (true) {
            try {
                val existing = repo.findById(key).orElse(null)
                val updated = if (existing == null || now.isAfter(existing.windowStart.plus(window))) {
                    RateLimitAttempt(id = key, count = 1, windowStart = now)
                } else {
                    existing.count += 1
                    existing
                }
                repo.save(updated)
                return updated.count
            } catch (ex: OptimisticLockingFailureException) {
                attempt++
                if (attempt >= 3) throw ex
            }
        }
    }

    @Transactional
    override fun deleteOlderThan(cutoff: Instant): Int = repo.deleteOlderThan(cutoff)
}
