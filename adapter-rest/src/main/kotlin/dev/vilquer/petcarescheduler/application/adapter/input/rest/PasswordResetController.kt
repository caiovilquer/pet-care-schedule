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
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@RestController
@RequestMapping("/api/v1/auth/password")
class PasswordResetController(
    private val passwordReset: PasswordResetUseCase,
    private val rateLimiter: RateLimiterService
) {
    data class ForgotReq(
        @field:EmailConstraint @field:NotBlank val email: String,
        @field:Size(max = 1_000) val returnUrl: String? = null,
    )
    data class ResetReq(
        @field:NotBlank val token: String,
        @field:NotBlank @field:Size(min = 8, max = 72) val newPassword: String
    )

    @PostMapping("/forgot")
    fun forgot(@Valid @RequestBody body: ForgotReq, request: HttpServletRequest): ResponseEntity<Void> {
        val key = rateLimitKey(request, body.email.trim().lowercase())
        rateLimiter.check(RateLimitAction.PASSWORD_RESET, key)
        passwordReset.requestReset(Email.of(body.email).getOrThrow(), body.returnUrl)
        return ResponseEntity.accepted().build()
    }

    @GetMapping("/reset/validate")
    fun validate(@RequestParam token: String, request: HttpServletRequest): ResponseEntity<Void> {
        val key = rateLimitKey(request, fingerprint(token))
        rateLimiter.check(RateLimitAction.PASSWORD_RESET, key)
        return if (passwordReset.validate(token)) ResponseEntity.ok().build()
        else ResponseEntity.badRequest().build()
    }

    @PostMapping("/reset")
    fun reset(@Valid @RequestBody body: ResetReq, request: HttpServletRequest): ResponseEntity<Void> {
        val key = rateLimitKey(request, fingerprint(body.token))
        rateLimiter.check(RateLimitAction.PASSWORD_RESET, key)
        passwordReset.reset(body.token, body.newPassword)
        return ResponseEntity.ok().build()
    }

    private fun rateLimitKey(request: HttpServletRequest, identifier: String?): String {
        val ip = request.remoteAddr ?: "unknown"
        val suffix = identifier?.trim()?.lowercase()
        return if (suffix.isNullOrBlank()) ip else "$ip:$suffix"
    }

    private fun fingerprint(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .take(12)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
