package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.result.PetDetailResult

fun interface GetPetUseCase {
    fun get(id: PetId, tutorId: TutorId): PetDetailResult
}