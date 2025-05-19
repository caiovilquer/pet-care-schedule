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
    val pet: Pet
) {
    fun markDone() = copy(status = Status.DONE)
}

@JvmInline value class EventId(val value: Long)
enum class EventType {VACCINE, MEDICINE, DIARY, BREED, SERVICE}
enum class Status { PENDING, DONE }