package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.usecase.command.CreatePetCommand
import dev.vilquer.petcarescheduler.usecase.result.PetCreatedResult
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdAccess

fun interface CreatePetUseCase {
    fun execute(cmd: CreatePetCommand, access: HouseholdAccess): PetCreatedResult
}
