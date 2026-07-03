package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.usecase.command.LoginCommand
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PasswordHashPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TokenIssuerPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.AuthUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.security.authentication.BadCredentialsException

@Service
open class AuthAppService(
    private val tutorRepo: TutorRepositoryPort,
    private val passwordHash: PasswordHashPort,
    private val tokenIssuer: TokenIssuerPort
) : AuthUseCase {

    @Transactional(readOnly = true)
    override fun authenticate(cmd: LoginCommand): String {

        val tutor = tutorRepo.findByEmail(cmd.email)
            ?: throw BadCredentialsException("Invalid e-mail or password")

        if (!passwordHash.matches(cmd.rawPassword, tutor.passwordHash)) {
            throw BadCredentialsException("Invalid e-mail or password")
        }

        return tokenIssuer.issueAccessToken(tutor)
    }
}
