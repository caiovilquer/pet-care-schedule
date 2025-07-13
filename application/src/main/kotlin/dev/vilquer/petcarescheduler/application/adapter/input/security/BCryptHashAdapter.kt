package dev.vilquer.petcarescheduler.application.adapter.input.security

import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PasswordHashPort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class BCryptHashAdapter(
    private val encoder: PasswordEncoder
) : PasswordHashPort {
    override fun hash(raw: CharSequence): String = encoder.encode(raw)
}