package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.entity.Event
import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId

interface EventRepositoryPort {
    fun save(event: Event): Event
    fun findById(id: EventId): Event?
    fun findByPetId(petId: PetId): List<Event>
    fun delete(id: EventId)

    fun listByTutor(tutorId: TutorId, page: Int, size: Int): List<Event>
    fun countByTutor(tutorId: TutorId): Long
    fun findByIdAndTutor(id: EventId, tutorId: TutorId): Event?
    fun existsForTutor(id: EventId, tutorId: TutorId): Boolean
}