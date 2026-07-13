package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.household.HouseholdAccess
import dev.vilquer.petcarescheduler.usecase.result.PetsPageResult

fun interface ListPetsUseCase {
    fun list(access: HouseholdAccess, page: Int, size: Int): PetsPageResult
}
