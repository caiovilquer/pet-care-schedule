package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.usecase.command.UpdateEventCommand
import dev.vilquer.petcarescheduler.usecase.result.EventDetailResult

fun interface UpdateEventUseCase {
    fun execute(cmd: UpdateEventCommand): EventDetailResult
}