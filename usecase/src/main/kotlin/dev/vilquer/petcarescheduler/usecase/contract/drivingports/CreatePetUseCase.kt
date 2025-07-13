package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.usecase.command.CreatePetCommand
import dev.vilquer.petcarescheduler.usecase.result.PetCreatedResult

fun interface CreatePetUseCase {
    fun execute(cmd: CreatePetCommand): PetCreatedResult
}