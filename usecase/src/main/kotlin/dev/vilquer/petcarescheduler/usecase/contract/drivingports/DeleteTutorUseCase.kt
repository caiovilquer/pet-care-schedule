package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.usecase.command.DeleteTutorCommand

fun interface DeleteTutorUseCase {
    fun execute(cmd: DeleteTutorCommand)
}