package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.valueobject.Email

interface PasswordResetUseCase {
    fun requestReset(email: Email)
    fun validate(token: String): Boolean
    fun reset(token: String, newPassword: String)
}