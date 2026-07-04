package dev.vilquer.petcarescheduler.usecase.command

import dev.vilquer.petcarescheduler.core.domain.valueobject.Email

data class LoginCommand(val email: Email, val rawPassword: String)