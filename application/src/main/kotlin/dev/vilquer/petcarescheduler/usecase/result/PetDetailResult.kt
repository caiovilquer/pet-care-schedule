package dev.vilquer.petcarescheduler.usecase.result

import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class PetDetailResult(
    val id: PetId,
    val name: String,
    val species: String,
    val breed: String?,
    val birthdate: LocalDate?,
    val photoUrl: String?,
    val photoAssetId: UUID? = null,
    val events: List<EventInfo>
) {
    data class EventInfo(
        val id: EventId,
        val type: EventType,
        val dateStart: LocalDateTime
    )
}
