package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.command.DeletePetCommand

fun interface DeletePetUseCase {
    fun execute(cmd: DeletePetCommand, tutorId: TutorId)
}