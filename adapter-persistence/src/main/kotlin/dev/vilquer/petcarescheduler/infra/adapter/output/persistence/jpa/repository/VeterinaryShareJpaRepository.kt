package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository

import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.VeterinaryShareJpa
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface VeterinaryShareJpaRepository : JpaRepository<VeterinaryShareJpa, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from VeterinaryShareJpa s where s.tokenHash = :hash and s.revokedAt is null")
    fun findActiveByHashForUpdate(@Param("hash") hash: String): VeterinaryShareJpa?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from VeterinaryShareJpa s where s.id = :id and s.householdId = :householdId")
    fun findForUpdate(@Param("id") id: UUID, @Param("householdId") householdId: UUID): VeterinaryShareJpa?

    @Query("""
        select s from VeterinaryShareJpa s
        where s.householdId = :householdId and (:petId is null or s.petId = :petId)
        order by s.createdAt desc, s.id desc
    """)
    fun list(@Param("householdId") householdId: UUID, @Param("petId") petId: Long?, pageable: Pageable): List<VeterinaryShareJpa>

    @Query("select count(s) from VeterinaryShareJpa s where s.householdId = :householdId and s.revokedAt is null and s.expiresAt > :now")
    fun countActive(@Param("householdId") householdId: UUID, @Param("now") now: Instant): Long
}
