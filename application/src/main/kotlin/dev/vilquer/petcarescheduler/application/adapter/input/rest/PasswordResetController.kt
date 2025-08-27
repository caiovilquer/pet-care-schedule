package dev.vilquer.petcarescheduler.application.adapter.input.rest


import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.PasswordResetUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth/password")
class PasswordResetController(
    private val passwordReset: PasswordResetUseCase
) {
    data class ForgotReq(val email: String)
    data class ResetReq(val token: String, val newPassword: String)

    @PostMapping("/forgot")
    fun forgot(@RequestBody body: ForgotReq): ResponseEntity<Void> {
        passwordReset.requestReset(Email(body.email))
        return ResponseEntity.accepted().build()
    }

    @GetMapping("/reset/validate")
    fun validate(@RequestParam token: String): ResponseEntity<Void> =
        if (passwordReset.validate(token)) ResponseEntity.ok().build()
        else ResponseEntity.badRequest().build()

    @PostMapping("/reset")
    fun reset(@RequestBody body: ResetReq): ResponseEntity<Void> {
        passwordReset.reset(body.token, body.newPassword)
        return ResponseEntity.ok().build()
    }
}
