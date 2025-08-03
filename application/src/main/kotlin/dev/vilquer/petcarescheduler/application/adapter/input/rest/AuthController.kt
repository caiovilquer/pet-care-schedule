package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.usecase.command.LoginCommand
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email as EmailVO
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.AuthUseCase
import jakarta.validation.constraints.Email
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthUseCase
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody dto: LoginDto): TokenDto =
        TokenDto(authService.authenticate(dto.toCmd()))

    data class LoginDto(
        @field:Email @field:NotBlank val email: String,
        @field:NotBlank val password: String
    ) {
        fun toCmd() = LoginCommand(EmailVO.of(email).getOrThrow(), password)
    }
    data class TokenDto(val token: String)
}