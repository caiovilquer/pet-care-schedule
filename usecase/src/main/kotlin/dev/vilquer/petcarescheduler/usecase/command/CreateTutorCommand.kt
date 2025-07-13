package dev.vilquer.petcarescheduler.usecase.command

data class CreateTutorCommand(
    val firstName: String,
    val lastName: String?,
    val email: String,
    val rawPassword: String,
    val phoneNumber: String,
    val avatar: String? = null
)