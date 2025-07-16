package dev.vilquer.petcarescheduler.usecase.contract.drivenports

fun interface PasswordHashPort {
    fun hash(raw: CharSequence): String
}