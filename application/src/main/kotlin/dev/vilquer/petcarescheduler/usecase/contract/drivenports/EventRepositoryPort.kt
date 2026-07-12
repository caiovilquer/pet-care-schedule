package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.entity.Event
import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import java.time.LocalDateTime

interface EventRepositoryPort {
    fun save(event: Event): Event
    fun findById(id: EventId): Event?
    fun findByPetId(petId: PetId): List<Event>
    fun delete(id: EventId)

    fun listByTutor(tutorId: TutorId, page: Int, size: Int): List<Event>
    fun countByTutor(tutorId: TutorId): Long
    fun findUpcomingByTutor(
        tutorId: TutorId,
        start: LocalDateTime,
        end: LocalDateTime,
        limit: Int,
    ): List<Event>
    fun findByIdAndTutor(id: EventId, tutorId: TutorId): Event?
    fun existsForTutor(id: EventId, tutorId: TutorId): Boolean
    fun findPendingReminders(start: LocalDateTime, end: LocalDateTime): List<EventReminderTarget>
}

data class EventReminderTarget(
    val event: Event,
    val tutorEmail: String,
    val petName: String?
)
