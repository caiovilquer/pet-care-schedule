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
    fun findByType(type: EventType): List<EventJpa>
    fun findByDateStart(start: LocalDateTime): List<EventJpa>
    fun findByStatus(status: Status): List<EventJpa>
    fun findAllByPetId(petId: Long): List<EventJpa>

    @Query(
        """
        select e
          from EventJpa e
          join PetJpa   p on p.id = e.petId
         where p.tutorId = :tutorId
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
}