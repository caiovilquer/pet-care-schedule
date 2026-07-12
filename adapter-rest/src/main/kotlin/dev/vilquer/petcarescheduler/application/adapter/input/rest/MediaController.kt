package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.application.adapter.input.security.CurrentJwt
import dev.vilquer.petcarescheduler.application.adapter.input.security.tutorId
import dev.vilquer.petcarescheduler.application.service.RateLimitAction
import dev.vilquer.petcarescheduler.application.service.RateLimiterService
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.media.MediaPurpose
import dev.vilquer.petcarescheduler.usecase.command.CompleteMediaUploadCommand
import dev.vilquer.petcarescheduler.usecase.command.DeleteMediaCommand
import dev.vilquer.petcarescheduler.usecase.command.InitiateMediaUploadCommand
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.MediaUploadUseCase
import dev.vilquer.petcarescheduler.usecase.result.MediaAssetResult
import dev.vilquer.petcarescheduler.usecase.result.MediaUploadInitiatedResult
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/v1/media")
class MediaController(
    private val media: MediaUploadUseCase,
    private val rateLimiter: RateLimiterService,
) {
    data class InitiateRequest(
        val purpose: MediaPurpose,
        @field:Positive val targetId: Long,
        @field:NotBlank @field:Size(max = 180) val filename: String,
        @field:Pattern(regexp = "^image/(jpeg|png)$") val contentType: String,
        @field:Min(1) @field:Max(5_242_880) val sizeBytes: Long,
        @field:Pattern(regexp = "^[A-Fa-f0-9]{64}$") val checksumSha256: String,
    )

    @PostMapping("/uploads")
    fun initiate(
        @Valid @RequestBody body: InitiateRequest,
        @AuthenticationPrincipal jwt: CurrentJwt,
        request: HttpServletRequest,
    ): ResponseEntity<MediaUploadInitiatedResult> {
        val tutorId = TutorId(jwt.tutorId())
        rateLimiter.check(RateLimitAction.MEDIA_UPLOAD, "${request.remoteAddr ?: "unknown"}:${tutorId.value}")
        val result = media.initiate(
            InitiateMediaUploadCommand(
                body.purpose, body.targetId, body.filename, body.contentType,
                body.sizeBytes, body.checksumSha256,
            ),
            tutorId,
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    @PostMapping("/uploads/{id}/complete")
    fun complete(
        @PathVariable id: UUID,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ): MediaAssetResult = media.complete(CompleteMediaUploadCommand(id), TutorId(jwt.tutorId()))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID, @AuthenticationPrincipal jwt: CurrentJwt) {
        media.delete(DeleteMediaCommand(id), TutorId(jwt.tutorId()))
    }

    @GetMapping("/{id}/content")
    fun content(@PathVariable id: UUID): ResponseEntity<Void> = ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(media.downloadUrl(id)))
        .header("Cache-Control", "private, max-age=300")
        .header("Referrer-Policy", "no-referrer")
        .build()
}
