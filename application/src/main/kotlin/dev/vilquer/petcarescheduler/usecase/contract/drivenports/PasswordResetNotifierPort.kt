package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import java.time.Duration

fun interface PasswordResetNotifierPort {
    /** Envia o link de redefinição de senha; o template e o remetente são detalhe do adapter. */
    fun sendResetLink(to: Email, tokenPlain: String, ttl: Duration, returnUrl: String?)
}
