package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository

import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.RateLimitAttempt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface RateLimitAttemptRepository : JpaRepository<RateLimitAttempt, String> {
    @Modifying
    @Query(
        """
        delete from RateLimitAttempt a
         where a.windowStart < :cutoff
        """
    )
    fun deleteOlderThan(@Param("cutoff") cutoff: Instant): Int
}
