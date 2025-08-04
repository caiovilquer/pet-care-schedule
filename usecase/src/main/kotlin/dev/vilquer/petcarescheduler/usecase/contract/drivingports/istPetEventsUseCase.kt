package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.result.EventDetailResult

fun interface ListPetEventsUseCase {
    fun list(petId: PetId, tutorId: TutorId): List<EventDetailResult>
}