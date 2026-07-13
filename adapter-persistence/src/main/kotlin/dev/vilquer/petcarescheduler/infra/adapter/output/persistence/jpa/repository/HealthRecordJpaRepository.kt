package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository

import dev.vilquer.petcarescheduler.core.domain.health.HealthRecordType
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.HealthRecordJpa
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface HealthRecordJpaRepository : JpaRepository<HealthRecordJpa, UUID> {
    @Query("""
        select r from HealthRecordJpa r
        where r.householdId = :householdId and r.occurredAt >= :from and r.occurredAt < :to
          and r.costAmount is not null and (:petId is null or r.petId = :petId)
        order by r.occurredAt desc, r.id desc
    """)
    fun findCostsByHousehold(
        @Param("householdId") householdId: UUID,
        @Param("petId") petId: Long?,
        @Param("from") from: Instant,
        @Param("to") to: Instant,
        pageable: org.springframework.data.domain.Pageable,
    ): List<HealthRecordJpa>
    @Query("select r from HealthRecordJpa r where r.id = :id and r.tutorId = :tutorId")
    fun findOwned(@Param("id") id: UUID, @Param("tutorId") tutorId: Long): HealthRecordJpa?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from HealthRecordJpa r where r.id = :id and r.tutorId = :tutorId")
    fun findOwnedForUpdate(@Param("id") id: UUID, @Param("tutorId") tutorId: Long): HealthRecordJpa?

    @Query("""
        select r from HealthRecordJpa r
        where r.tutorId = :tutorId and r.petId = :petId
          and r.occurredAt >= :from and r.occurredAt < :to
          and (:type is null or r.type = :type)
        order by r.occurredAt desc, r.id desc
    """)
    fun search(
        @Param("tutorId") tutorId: Long,
        @Param("petId") petId: Long,
        @Param("from") from: Instant,
        @Param("to") to: Instant,
        @Param("type") type: HealthRecordType?,
        pageable: Pageable,
    ): Page<HealthRecordJpa>

    fun findByIdAndHouseholdId(id: UUID, householdId: UUID): HealthRecordJpa?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from HealthRecordJpa r where r.id = :id and r.householdId = :householdId")
    fun findByHouseholdForUpdate(@Param("id") id: UUID, @Param("householdId") householdId: UUID): HealthRecordJpa?

    @Query("""
        select r from HealthRecordJpa r where r.householdId = :householdId and r.petId = :petId
          and r.occurredAt >= :from and r.occurredAt < :to and (:type is null or r.type = :type)
        order by r.occurredAt desc, r.id desc
    """)
    fun searchByHousehold(@Param("householdId") householdId: UUID, @Param("petId") petId: Long,
        @Param("from") from: Instant, @Param("to") to: Instant, @Param("type") type: HealthRecordType?, pageable: Pageable): Page<HealthRecordJpa>
}
