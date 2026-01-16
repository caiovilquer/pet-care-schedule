package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.usecase.command.LoginCommand
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email as EmailVO
import dev.vilquer.petcarescheduler.application.service.RateLimitAction
import dev.vilquer.petcarescheduler.application.service.RateLimiterService
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.AuthUseCase
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthUseCase,
    private val rateLimiter: RateLimiterService
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody dto: LoginDto, request: HttpServletRequest): ResponseEntity<TokenDto> {
        val key = rateLimitKey("login", request, dto.email)
        rateLimiter.check(RateLimitAction.LOGIN, key)
        val token = authService.authenticate(dto.toCmd())
        return ResponseEntity.ok()
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .body(TokenDto(token))
    }

    data class LoginDto(
        @field:Email @field:NotBlank val email: String,
        @field:NotBlank val password: String
    ) {
        fun toCmd() = LoginCommand(EmailVO.of(email).getOrThrow(), password)
    }
    data class TokenDto(
        val token: String,
        val tokenType: String = "Bearer",
        val accessToken: String = token,
        @JsonProperty("access_token") val accessTokenCompat: String = token,
        @JsonProperty("token_type") val tokenTypeCompat: String = tokenType
    )

    private fun rateLimitKey(action: String, request: HttpServletRequest, email: String): String {
        val ip = request.remoteAddr ?: "unknown"
        return "$action:$ip:${email.trim().lowercase()}"
    }
}
