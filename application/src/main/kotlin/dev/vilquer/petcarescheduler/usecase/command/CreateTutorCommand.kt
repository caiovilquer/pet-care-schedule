package dev.vilquer.petcarescheduler.usecase.command

import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber

data class CreateTutorCommand(
    val firstName: String,
    val lastName: String?,
    val email: Email,
    val rawPassword: String,
    val phoneNumber: PhoneNumber?,
    val avatar: String? = null
)