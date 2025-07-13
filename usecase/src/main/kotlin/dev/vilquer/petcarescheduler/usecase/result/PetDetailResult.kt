package dev.vilquer.petcarescheduler.usecase.result

import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import java.time.LocalDate
import java.time.ZonedDateTime

data class PetDetailResult(
    val id: PetId,
    val name: String,
    val specie: String,
    val race: String?,
    val birthdate: LocalDate,
    val events: List<EventInfo>
) {
    data class EventInfo(
        val id: EventId,
        val type: EventType,
        val dateStart: ZonedDateTime
    )
}