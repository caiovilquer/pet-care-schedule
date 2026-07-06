package dev.vilquer.petcarescheduler.core.domain.entity

import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import java.time.LocalDateTime

data class Event(
    val id: EventId? = null,
    val type: EventType,
    val description: String?,
    val dateStart: LocalDateTime,
    val recurrence: Recurrence?,
    val status: Status = Status.PENDING,
    val occurrenceCount: Int = 0,
    val petId: PetId?
) {
    fun markPending() = copy(status = Status.PENDING)

    /**
     * Conclui o evento e, se houver recorrência ativa, calcula a próxima
     * ocorrência (um novo Event, PENDING, ainda sem id). Sem recorrência, ou
     * com a série esgotada (repetitions/finalDate), [EventCompletion.next] é null.
     */
    fun complete(): EventCompletion {
        val completed = copy(status = Status.DONE)
        val next = recurrence?.let { rec ->
            val nextCount = occurrenceCount + 1
            val nextDate = rec.nextOccurrence(dateStart)
            if (rec.hasNext(nextCount, nextDate)) {
                Event(
                    type = type,
                    description = description,
                    dateStart = nextDate,
                    recurrence = recurrence,
                    status = Status.PENDING,
                    occurrenceCount = nextCount,
                    petId = petId
                )
            } else {
                null
            }
        }
        return EventCompletion(completed, next)
    }
}

data class EventCompletion(val completed: Event, val next: Event?)

@JvmInline value class EventId(val value: Long)
enum class EventType {VACCINE, MEDICINE, DIARY, BREED, SERVICE}
enum class Status { PENDING, DONE }
