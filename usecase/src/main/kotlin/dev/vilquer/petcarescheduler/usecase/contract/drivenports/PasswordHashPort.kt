package dev.vilquer.petcarescheduler.usecase.contract.drivenports

interface PasswordHashPort {
    fun hash(raw: CharSequence): String
    fun matches(raw: CharSequence, hash: String): Boolean
}