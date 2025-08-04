package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.command.ToggleEventCommand

interface ToggleEventUseCase {
    fun execute(cmd: ToggleEventCommand, tutorId: TutorId)
}