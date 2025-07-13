package dev.vilquer.petcarescheduler.infra.adapter.output.external

import dev.vilquer.petcarescheduler.core.domain.entity.Event
import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.toDomain
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.toJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.EventJpaRepository
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.EventRepositoryPort
import org.springframework.stereotype.Repository

@Repository
class EventRepositoryAdapter(
    private val jpa: EventJpaRepository
) : EventRepositoryPort {

    /* --------- core ➜ infra --------- */
    override fun save(event: Event): Event =
        jpa.save(event.toJpa()).toDomain()

    /* --------- infra ➜ core --------- */
    override fun findById(id: EventId): Event? =
        jpa.findById(id.value).orElse(null)?.toDomain()

    override fun findByPetId(petId: PetId): List<Event> =
        jpa.findAllByPetId(petId.value).map { it.toDomain() }

//    override fun findByDate(date: LocalDate): List<Event> =
//        jpa.findByDateStartBetween(
//            date.atStartOfDay(ZoneId.systemDefault()),
//            date.plusDays(1).atStartOfDay(ZoneId.systemDefault())
//        ).map { it.toDomain() }

}