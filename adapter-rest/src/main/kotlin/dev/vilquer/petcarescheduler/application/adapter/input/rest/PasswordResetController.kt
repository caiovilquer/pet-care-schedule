package dev.vilquer.petcarescheduler.application.adapter.input.rest


import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.application.service.RateLimitAction
import dev.vilquer.petcarescheduler.application.service.RateLimiterService
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.PasswordResetUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Email as EmailConstraint
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@RestController
@RequestMapping("/api/v1/auth/password")
class PasswordResetController(
    private val passwordReset: PasswordResetUseCase,
    private val rateLimiter: RateLimiterService
) {
    data class ForgotReq(
        @field:EmailConstraint @field:NotBlank val email: String
    )
    data class ResetReq(
        @field:NotBlank val token: String,
        @field:NotBlank @field:Size(min = 8, max = 72) val newPassword: String
    )

    @PostMapping("/forgot")
    fun forgot(@Valid @RequestBody body: ForgotReq, request: HttpServletRequest): ResponseEntity<Void> {
        val key = rateLimitKey("reset-forgot", request, body.email)
        rateLimiter.check(RateLimitAction.PASSWORD_RESET, key)
        passwordReset.requestReset(Email.of(body.email).getOrThrow())
        return ResponseEntity.accepted().build()
    }

    @GetMapping("/reset/validate")
    fun validate(@RequestParam token: String, request: HttpServletRequest): ResponseEntity<Void> {
        val key = rateLimitKey("reset-validate", request, null)
        rateLimiter.check(RateLimitAction.PASSWORD_RESET, key)
        return if (passwordReset.validate(token)) ResponseEntity.ok().build()
        else ResponseEntity.badRequest().build()
    }

    @PostMapping("/reset")
    fun reset(@Valid @RequestBody body: ResetReq, request: HttpServletRequest): ResponseEntity<Void> {
        val key = rateLimitKey("reset", request, body.token)
        rateLimiter.check(RateLimitAction.PASSWORD_RESET, key)
        passwordReset.reset(body.token, body.newPassword)
        return ResponseEntity.ok().build()
    }

    private fun rateLimitKey(action: String, request: HttpServletRequest, identifier: String?): String {
        val ip = request.remoteAddr ?: "unknown"
        val suffix = identifier?.trim()?.lowercase()
        return if (suffix.isNullOrBlank()) "$action:$ip" else "$action:$ip:$suffix"
    }
}
