package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.usecase.command.LoginCommand
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email as EmailVO
import dev.vilquer.petcarescheduler.application.adapter.input.security.CurrentJwt
import dev.vilquer.petcarescheduler.application.adapter.input.security.RefreshProperties
import dev.vilquer.petcarescheduler.application.adapter.input.security.tutorId
import dev.vilquer.petcarescheduler.application.exception.InvalidCredentialsException
import dev.vilquer.petcarescheduler.application.service.RateLimitAction
import dev.vilquer.petcarescheduler.application.service.RateLimiterService
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.AuthTokens
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.AuthUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.SessionUseCase
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.WebUtils
import jakarta.servlet.http.HttpServletRequest
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthUseCase,
    private val sessionService: SessionUseCase,
    private val rateLimiter: RateLimiterService,
    private val refreshProperties: RefreshProperties
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody dto: LoginDto, request: HttpServletRequest): ResponseEntity<TokenDto> {
        val key = rateLimitKey(request, dto.email.trim().lowercase())
        rateLimiter.check(RateLimitAction.LOGIN, key)
        val tokens = authService.authenticate(dto.toCmd())
        rateLimiter.reset(RateLimitAction.LOGIN, key)
        return respondWithTokens(tokens)
    }

    @PostMapping("/refresh")
    fun refresh(request: HttpServletRequest): ResponseEntity<TokenDto> {
        val raw = readRefreshCookie(request)
            ?: throw InvalidCredentialsException("Missing refresh token")
        val key = rateLimitKey(request, fingerprint(raw))
        rateLimiter.check(RateLimitAction.TOKEN_REFRESH, key)
        val tokens = sessionService.refresh(raw, request.getHeader(HttpHeaders.USER_AGENT))
        rateLimiter.reset(RateLimitAction.TOKEN_REFRESH, key)
        return respondWithTokens(tokens)
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<Void> {
        readRefreshCookie(request)?.let { sessionService.logout(it) }
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, expireCookie().toString())
            .build()
    }

    @PostMapping("/logout-all")
    fun logoutAll(@AuthenticationPrincipal jwt: CurrentJwt): ResponseEntity<Void> {
        sessionService.logoutAll(TutorId(jwt.tutorId()))
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, expireCookie().toString())
            .build()
    }

    private fun respondWithTokens(tokens: AuthTokens): ResponseEntity<TokenDto> {
        return ResponseEntity.ok()
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokens.accessToken}")
            .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens).toString())
            .body(TokenDto(tokens.accessToken))
    }

    private fun refreshCookie(tokens: AuthTokens): ResponseCookie {
        val cfg = refreshProperties.cookie
        return ResponseCookie.from(cfg.name, tokens.refreshToken)
            .httpOnly(true)
            .secure(cfg.secure)
            .sameSite(cfg.sameSite)
            .path(cfg.path)
            .maxAge(refreshProperties.ttl)
            .apply { cfg.domain?.takeIf { it.isNotBlank() }?.let { domain(it) } }
            .build()
    }

    private fun expireCookie(): ResponseCookie {
        val cfg = refreshProperties.cookie
        return ResponseCookie.from(cfg.name, "")
            .httpOnly(true)
            .secure(cfg.secure)
            .sameSite(cfg.sameSite)
            .path(cfg.path)
            .maxAge(0)
            .apply { cfg.domain?.takeIf { it.isNotBlank() }?.let { domain(it) } }
            .build()
    }

    private fun readRefreshCookie(request: HttpServletRequest): String? =
        WebUtils.getCookie(request, refreshProperties.cookie.name)?.value

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

    private fun rateLimitKey(request: HttpServletRequest, identifier: String): String {
        val ip = request.remoteAddr ?: "unknown"
        return "$ip:$identifier"
    }

    private fun fingerprint(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .take(12)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
