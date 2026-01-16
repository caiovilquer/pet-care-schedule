package dev.vilquer.petcarescheduler.infra.adapter.output.reset.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.*

interface PasswordResetTokenRepository :
    JpaRepository<PasswordResetTokenJpa, UUID> {
    fun findByTokenHashAndUsedAtIsNull(tokenHash: String): PasswordResetTokenJpa?

    @Modifying
    @Query(
        """
        delete from PasswordResetTokenJpa t
         where t.usedAt is not null
            or t.expiresAt < :now
        """
    )
    fun deleteExpiredOrUsed(@Param("now") now: Instant): Int

    @Modifying
    @Query(
        """
        update PasswordResetTokenJpa t
           set t.usedAt = :usedAt
         where t.userId = :userId
           and t.usedAt is null
        """
    )
    fun invalidateByUserId(
        @Param("userId") userId: Long,
        @Param("usedAt") usedAt: Instant
    ): Int
}
