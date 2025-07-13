package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.usecase.command.CreateTutorCommand
import dev.vilquer.petcarescheduler.usecase.result.TutorCreatedResult

fun interface CreateTutorUseCase {
    fun execute(cmd: CreateTutorCommand): TutorCreatedResult
}