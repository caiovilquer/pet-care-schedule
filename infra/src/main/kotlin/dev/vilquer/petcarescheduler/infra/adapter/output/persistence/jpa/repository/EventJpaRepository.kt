package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository

import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.Status
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.EventJpa
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface EventJpaRepository: JpaRepository<EventJpa, Long> {
    fun findByType (type: EventType): List<EventJpa>
    fun findByDateStart(start: LocalDateTime): List<EventJpa>
    fun findByStatus(status: Status): List<EventJpa>
    fun findAllByPetId(petId: Long): List<EventJpa>
}