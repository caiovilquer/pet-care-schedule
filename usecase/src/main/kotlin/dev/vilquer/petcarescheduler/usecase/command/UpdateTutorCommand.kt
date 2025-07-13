package dev.vilquer.petcarescheduler.usecase.command

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId

data class UpdateTutorCommand(
    val tutorId: TutorId,
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val avatar: String? = null
)