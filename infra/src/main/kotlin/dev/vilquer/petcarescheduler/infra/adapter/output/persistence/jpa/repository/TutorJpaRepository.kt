package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository

import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.TutorJpa
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface TutorJpaRepository: JpaRepository<TutorJpa,Long> {
    fun findByEmail(email: String): TutorJpa?
    fun findByPhoneNumber(phoneNumber: String): TutorJpa?


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update TutorJpa t
           set t.passwordHash = :passwordHash,
               t.passwordChangedAt = :whenUtc
         where t.id = :id
    """)
    fun setPasswordAndBumpChangedAt(
        @Param("id") id: Long,
        @Param("passwordHash") passwordHash: String,
        @Param("whenUtc") whenUtc: Instant
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update TutorJpa t
           set t.passwordChangedAt = :whenUtc
         where t.id = :id
    """)
    fun bumpChangedAt(
        @Param("id") id: Long,
        @Param("whenUtc") whenUtc: Instant
    ): Int
}