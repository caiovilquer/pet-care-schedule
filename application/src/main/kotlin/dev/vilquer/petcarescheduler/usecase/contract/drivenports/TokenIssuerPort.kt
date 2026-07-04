package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.entity.Tutor

fun interface TokenIssuerPort {
    /** Emite o token de acesso (JWT hoje) para o tutor autenticado. */
    fun issueAccessToken(tutor: Tutor): String
}
