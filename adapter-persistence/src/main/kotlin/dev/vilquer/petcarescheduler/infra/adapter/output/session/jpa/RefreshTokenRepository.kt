package dev.vilquer.petcarescheduler.infra.adapter.output.session.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshTokenJpa, UUID> {
    fun findByTokenHash(tokenHash: String): RefreshTokenJpa?

    @Modifying
    @Query(
        """
        update RefreshTokenJpa t
           set t.revokedAt = :at
         where t.familyId = :familyId
           and t.revokedAt is null
        """
    )
    fun revokeByFamilyId(@Param("familyId") familyId: UUID, @Param("at") at: Instant): Int

    @Modifying
    @Query(
        """
        update RefreshTokenJpa t
           set t.revokedAt = :at
         where t.userId = :userId
           and t.revokedAt is null
        """
    )
    fun revokeByUserId(@Param("userId") userId: Long, @Param("at") at: Instant): Int

    @Modifying
    @Query(
        """
        delete from RefreshTokenJpa t
         where t.revokedAt is not null
            or t.usedAt is not null
            or t.expiresAt < :now
        """
    )
    fun deleteExpiredOrRevoked(@Param("now") now: Instant): Int
}
