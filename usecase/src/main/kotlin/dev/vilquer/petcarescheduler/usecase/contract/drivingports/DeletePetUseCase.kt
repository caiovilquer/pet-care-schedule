package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.usecase.command.DeletePetCommand

fun interface DeletePetUseCase {
    fun execute(cmd: DeletePetCommand)
}