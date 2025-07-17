package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.usecase.command.LoginCommand
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.AuthUseCase
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthUseCase
) {
    @PostMapping("/login")
    fun login(@RequestBody dto: LoginDto): TokenDto =
        TokenDto(authService.authenticate(dto.toCmd()))

    data class LoginDto(val email: String, val password: String) {
        fun toCmd() = LoginCommand(Email.of(email).getOrThrow(), password)
    }
    data class TokenDto(val token: String)
}