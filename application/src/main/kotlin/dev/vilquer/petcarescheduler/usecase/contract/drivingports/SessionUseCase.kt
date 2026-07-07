package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId

interface SessionUseCase {
    fun refresh(rawRefreshToken: String, userAgent: String? = null): AuthTokens
    fun logout(rawRefreshToken: String)
    fun logoutAll(userId: TutorId)
}
