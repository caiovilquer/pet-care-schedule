package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.result.EventsPageResult

fun interface ListEventsUseCase {
    fun list(tutorId: TutorId, page: Int, size: Int): EventsPageResult
}