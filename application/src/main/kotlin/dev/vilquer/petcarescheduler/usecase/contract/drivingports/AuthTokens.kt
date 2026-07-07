package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import java.time.Instant

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val refreshExpiresAt: Instant,
)
