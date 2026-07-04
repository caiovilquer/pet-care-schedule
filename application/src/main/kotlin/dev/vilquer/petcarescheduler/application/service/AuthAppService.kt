package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.exception.InvalidCredentialsException
import dev.vilquer.petcarescheduler.usecase.command.LoginCommand
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PasswordHashPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TokenIssuerPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.AuthUseCase

class AuthAppService(
    private val tutorRepo: TutorRepositoryPort,
    private val passwordHash: PasswordHashPort,
    private val tokenIssuer: TokenIssuerPort
) : AuthUseCase {

    override fun authenticate(cmd: LoginCommand): String {

        val tutor = tutorRepo.findByEmail(cmd.email)
            ?: throw InvalidCredentialsException("Invalid e-mail or password")

        if (!passwordHash.matches(cmd.rawPassword, tutor.passwordHash)) {
            throw InvalidCredentialsException("Invalid e-mail or password")
        }

        return tokenIssuer.issueAccessToken(tutor)
    }
}
