package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.usecase.command.DeleteEventCommand

fun interface DeleteEventUseCase {
    fun execute(cmd: DeleteEventCommand)
}