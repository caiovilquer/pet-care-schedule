package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.result.TutorDetailResult

fun interface GetTutorUseCase {
    fun get(id: TutorId): TutorDetailResult
}