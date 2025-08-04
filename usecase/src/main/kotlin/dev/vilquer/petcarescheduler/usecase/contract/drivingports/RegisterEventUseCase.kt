package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.command.RegisterEventCommand
import dev.vilquer.petcarescheduler.usecase.result.EventRegisteredResult

fun interface RegisterEventUseCase {
    fun execute(cmd: RegisterEventCommand, tutorId: TutorId): EventRegisteredResult
}