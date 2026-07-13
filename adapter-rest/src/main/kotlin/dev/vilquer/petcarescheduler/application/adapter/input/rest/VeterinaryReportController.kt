package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.application.adapter.input.security.CurrentHousehold
import dev.vilquer.petcarescheduler.application.adapter.input.security.CurrentJwt
import dev.vilquer.petcarescheduler.application.adapter.input.security.tutorId
import dev.vilquer.petcarescheduler.application.service.RateLimitAction
import dev.vilquer.petcarescheduler.application.service.RateLimiterService
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.report.VeterinaryShareId
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.VeterinaryReportUseCase
import dev.vilquer.petcarescheduler.usecase.result.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

data class VeterinaryShareCreateRequest(
    @field:Positive val petId: Long,
    @field:NotBlank @field:Size(max = 100) val label: String,
    val from: LocalDate,
    val to: LocalDate,
    @field:Positive @field:Max(720) val expiresInHours: Long,
    val includeNotes: Boolean = false,
    val includeCosts: Boolean = false,
    val includeDocuments: Boolean = false,
)
data class VeterinaryShareResolveRequest(@field:NotBlank @field:Size(min = 32, max = 128) val token: String)
data class VeterinarySharedAttachmentRequest(
    @field:NotBlank @field:Size(min = 32, max = 128) val token: String,
    val mediaId: UUID,
)
data class SharedAttachmentUrlResponse(val url: String)

@RestController
@RequestMapping("/api/v1")
@Validated
class VeterinaryReportController(
    private val reports: VeterinaryReportUseCase,
    private val household: CurrentHousehold,
    private val limiter: RateLimiterService,
) {
    @GetMapping("/pets/{petId}/veterinary-summary")
    fun summary(
        @PathVariable petId: Long,
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ) = reports.summary(VeterinarySummaryQuery(PetId(petId), from, to), household.resolve(jwt))

    @PostMapping("/veterinary-shares")
    fun createShare(
        @Valid @RequestBody body: VeterinaryShareCreateRequest,
        @AuthenticationPrincipal jwt: CurrentJwt,
        request: HttpServletRequest,
    ): ResponseEntity<VeterinaryShareCreatedResult> {
        val access = household.resolve(jwt)
        limiter.check(RateLimitAction.VETERINARY_SHARE_CREATE, "${request.remoteAddr}:${jwt.tutorId()}:${access.householdId.value}")
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(reports.createShare(
            CreateVeterinaryShareCommand(
                PetId(body.petId), body.label, body.from, body.to, body.expiresInHours,
                body.includeNotes, body.includeCosts, body.includeDocuments,
            ), access,
        ))
    }

    @GetMapping("/veterinary-shares")
    fun listShares(@RequestParam(required = false) petId: Long?, @AuthenticationPrincipal jwt: CurrentJwt) =
        reports.listShares(petId?.let(::PetId), household.resolve(jwt))

    @DeleteMapping("/veterinary-shares/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revoke(
        @PathVariable id: UUID,
        @RequestParam @PositiveOrZero version: Long,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ) = reports.revoke(RevokeVeterinaryShareCommand(VeterinaryShareId(id), version), household.resolve(jwt))
}

@RestController
@RequestMapping("/api/v1/public/veterinary-summary")
@Validated
class PublicVeterinaryReportController(
    private val reports: VeterinaryReportUseCase,
    private val limiter: RateLimiterService,
) {
    @PostMapping
    fun resolve(@Valid @RequestBody body: VeterinaryShareResolveRequest, request: HttpServletRequest): ResponseEntity<PublicVeterinarySummaryResult> {
        limit(request)
        return secure(reports.publicSummary(ResolveVeterinaryShareCommand(body.token)))
    }

    @PostMapping("/attachment-url")
    fun attachment(
        @Valid @RequestBody body: VeterinarySharedAttachmentRequest,
        request: HttpServletRequest,
    ): ResponseEntity<SharedAttachmentUrlResponse> {
        limit(request)
        return secure(SharedAttachmentUrlResponse(reports.sharedAttachmentUrl(SharedAttachmentUrlCommand(body.token, body.mediaId))))
    }

    private fun limit(request: HttpServletRequest) {
        limiter.check(RateLimitAction.VETERINARY_SHARE_ACCESS, request.remoteAddr ?: "unknown")
    }

    private fun <T> secure(value: T): ResponseEntity<T> = ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header("Pragma", "no-cache")
        .header("Referrer-Policy", "no-referrer")
        .header("X-Content-Type-Options", "nosniff")
        .header("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")
        .body(value)
}
