package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.result.EventDetailResult

fun interface GetEventUseCase {
    fun get(id: EventId, tutorId: TutorId): EventDetailResult
}