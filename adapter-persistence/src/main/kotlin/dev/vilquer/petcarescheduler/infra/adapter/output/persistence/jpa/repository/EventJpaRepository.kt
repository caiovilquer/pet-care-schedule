package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository

import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.Status
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.EventJpa
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface EventJpaRepository : JpaRepository<EventJpa, Long> {
    fun findAllByPetIdOrderByDateStartAscIdAsc(petId: Long): List<EventJpa>

    @Query(
        """
        select e
          from EventJpa e
          join PetJpa   p on p.id = e.petId
         where p.tutorId = :tutorId
         order by e.dateStart asc, e.id asc
    """
    )
    fun findAllByTutorId(
        @Param("tutorId") tutorId: Long,
        pageable: Pageable
    ): Page<EventJpa>

    @Query(
        """
        select e
          from EventJpa e
          join PetJpa   p on p.id = e.petId
         where e.id = :id and p.tutorId = :tutorId
    """
    )
    fun findByIdAndTutorId(
        @Param("id") id: Long,
        @Param("tutorId") tutorId: Long
    ): EventJpa?

    @Query(
        """
        select case when count(e) > 0 then true else false end
          from EventJpa e
          join PetJpa   p on p.id = e.petId
         where e.id = :id and p.tutorId = :tutorId
    """
    )
    fun existsByIdAndTutorId(
        @Param("id") id: Long,
        @Param("tutorId") tutorId: Long
    ): Boolean

    @Query(
        """
        select count(e)
          from EventJpa e
          join PetJpa   p on p.id = e.petId
         where p.tutorId = :tutorId
    """
    )
    fun countByTutorId(@Param("tutorId") tutorId: Long): Long

    @Query(
        """
        select e
          from EventJpa e
          join PetJpa p on p.id = e.petId
         where p.tutorId = :tutorId
           and e.status = :status
           and e.dateStart >= :start
           and e.dateStart <= :end
         order by e.dateStart asc, e.id asc
        """
    )
    fun findUpcomingByTutorId(
        @Param("tutorId") tutorId: Long,
        @Param("status") status: Status,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
        pageable: Pageable,
    ): List<EventJpa>

    @Query(
        """
        select e as event, t.email as tutorEmail, p.name as petName, h.timezone as timezone
          from EventJpa e
          join PetJpa   p on p.id = e.petId
          join TutorJpa t on t.id = p.tutorId
          join HouseholdJpa h on h.id = p.householdId
         where e.status = :status
           and e.dateStart >= :start
           and e.dateStart < :end
    """
    )
    fun findReminderTargets(
        @Param("status") status: Status,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): List<EventReminderTargetRow>
}

interface EventReminderTargetRow {
    val event: EventJpa
    val tutorEmail: String
    val petName: String
    val timezone: String?
}
