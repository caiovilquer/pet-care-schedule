package dev.vilquer.petcarescheduler.usecase.command

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber

data class UpdateTutorCommand(
    val tutorId: TutorId,
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: PhoneNumber? = null,
    val avatar: String? = null
)