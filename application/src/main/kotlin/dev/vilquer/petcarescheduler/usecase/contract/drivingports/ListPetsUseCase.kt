package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.result.PetsPageResult

fun interface ListPetsUseCase {
    fun list(tutorId: TutorId, page: Int, size: Int): PetsPageResult
}