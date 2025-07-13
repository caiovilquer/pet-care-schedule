package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.usecase.command.UpdateTutorCommand
import dev.vilquer.petcarescheduler.usecase.result.TutorDetailResult

fun interface UpdateTutorUseCase {
    fun execute(cmd: UpdateTutorCommand): TutorDetailResult
}