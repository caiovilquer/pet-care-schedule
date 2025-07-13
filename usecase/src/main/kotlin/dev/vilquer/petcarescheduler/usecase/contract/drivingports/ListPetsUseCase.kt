package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.usecase.result.PetsPageResult

fun interface ListPetsUseCase {
    fun list(page: Int, size: Int): PetsPageResult
}