package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.adapter.input.security.JwtProperties
import dev.vilquer.petcarescheduler.usecase.command.LoginCommand
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.AuthUseCase
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import io.jsonwebtoken.Jwts
import org.springframework.transaction.annotation.Transactional
import org.springframework.security.authentication.BadCredentialsException
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey


@Service
open class AuthAppService(
    private val tutorRepo: TutorRepositoryPort,
    private val encoder: PasswordEncoder,
    private val signingKey: SecretKey,
    private val props: JwtProperties
) : AuthUseCase {

    @Transactional(readOnly = true)
    override fun authenticate(cmd: LoginCommand): String {

        val tutor = tutorRepo.findByEmail(cmd.email)
            ?: throw BadCredentialsException("Invalid e-mail or password")

        if (!encoder.matches(cmd.rawPassword, tutor.passwordHash)) {
            throw BadCredentialsException("Invalid e-mail or password")
        }

        val now = Instant.now()
        val expirationTime = now.plus(props.expiration)

        val builder = Jwts.builder()
            .issuer(props.issuer)
            .signWith(signingKey)

        return builder
            .subject(tutor.id!!.value.toString())
            .claim("name", tutor.firstName)
            .claim("role", "TUTOR")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expirationTime))
            .compact()
    }
}
