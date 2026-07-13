package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository

import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurementType
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.HealthMeasurementJpa
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface HealthMeasurementJpaRepository : JpaRepository<HealthMeasurementJpa, UUID> {
    @Query("select m from HealthMeasurementJpa m where m.id = :id and m.tutorId = :tutorId")
    fun findOwned(@Param("id") id: UUID, @Param("tutorId") tutorId: Long): HealthMeasurementJpa?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from HealthMeasurementJpa m where m.id = :id and m.tutorId = :tutorId")
    fun findOwnedForUpdate(@Param("id") id: UUID, @Param("tutorId") tutorId: Long): HealthMeasurementJpa?

    @Query("""
        select m from HealthMeasurementJpa m
        where m.tutorId = :tutorId and m.petId = :petId
          and m.measuredAt >= :from and m.measuredAt < :to
          and (:type is null or m.type = :type)
        order by m.measuredAt asc, m.id asc
    """)
    fun findSeries(
        @Param("tutorId") tutorId: Long,
        @Param("petId") petId: Long,
        @Param("type") type: HealthMeasurementType?,
        @Param("from") from: Instant,
        @Param("to") to: Instant,
        pageable: Pageable,
    ): List<HealthMeasurementJpa>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from HealthMeasurementJpa m where m.id = :id and m.householdId = :householdId")
    fun findByHouseholdForUpdate(@Param("id") id: UUID, @Param("householdId") householdId: UUID): HealthMeasurementJpa?

    @Query("""
        select m from HealthMeasurementJpa m where m.householdId = :householdId and m.petId = :petId
          and m.measuredAt >= :from and m.measuredAt < :to and (:type is null or m.type = :type)
        order by m.measuredAt asc, m.id asc
    """)
    fun findSeriesByHousehold(@Param("householdId") householdId: UUID, @Param("petId") petId: Long,
        @Param("type") type: HealthMeasurementType?, @Param("from") from: Instant,
        @Param("to") to: Instant, pageable: Pageable): List<HealthMeasurementJpa>
}
