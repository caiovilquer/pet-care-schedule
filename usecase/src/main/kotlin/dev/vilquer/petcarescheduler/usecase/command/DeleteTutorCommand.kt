package dev.vilquer.petcarescheduler.usecase.command

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId

data class DeleteTutorCommand(val tutorId: TutorId)