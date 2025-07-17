package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.entity.Event
import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.core.domain.entity.PetId

interface EventRepositoryPort {
    fun save(event: Event): Event
    fun findById(id: EventId): Event?
    fun findByPetId(petId: PetId): List<Event>
    fun delete(id: EventId)
}