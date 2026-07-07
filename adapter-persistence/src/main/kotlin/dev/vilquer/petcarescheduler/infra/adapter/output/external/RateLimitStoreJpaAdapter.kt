package dev.vilquer.petcarescheduler.infra.adapter.output.external

import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.RateLimitAttempt
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.RateLimitAttemptRepository
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.RateLimitStorePort
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Repository
class RateLimitStoreJpaAdapter(
    private val repo: RateLimitAttemptRepository,
) : RateLimitStorePort {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Transactional
    override fun registerAttempt(key: String, now: Instant, window: Duration): Int {
        repeat(MAX_RETRIES) { attempt ->
            try {
                return persistAttempt(key, now, window)
            } catch (ex: OptimisticLockingFailureException) {
                entityManager.clear()
                if (attempt == MAX_RETRIES - 1) throw ex
            } catch (ex: DataIntegrityViolationException) {
                // Corrida rara no primeiro INSERT da chave: outra thread
                // registrou entre o findById e o save.
                entityManager.clear()
                if (attempt == MAX_RETRIES - 1) throw ex
            }
        }
        error("unreachable")
    }

    private fun persistAttempt(key: String, now: Instant, window: Duration): Int {
        val existing = repo.findById(key).orElse(null)
        val updated = when {
            existing == null -> RateLimitAttempt(id = key, count = 1, windowStart = now)
            now.isAfter(existing.windowStart.plus(window)) -> {
                existing.count = 1
                existing.windowStart = now
                existing
            }
            else -> {
                existing.count += 1
                existing
            }
        }
        repo.save(updated)
        return updated.count
    }

    companion object {
        private const val MAX_RETRIES = 10
    }

    @Transactional
    override fun deleteOlderThan(cutoff: Instant): Int = repo.deleteOlderThan(cutoff)
}
