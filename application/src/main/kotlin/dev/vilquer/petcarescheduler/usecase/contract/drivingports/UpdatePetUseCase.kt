package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.household.HouseholdAccess
import dev.vilquer.petcarescheduler.usecase.command.UpdatePetCommand
import dev.vilquer.petcarescheduler.usecase.result.PetDetailResult

fun interface UpdatePetUseCase {
    fun execute(cmd: UpdatePetCommand, access: HouseholdAccess): PetDetailResult
}
