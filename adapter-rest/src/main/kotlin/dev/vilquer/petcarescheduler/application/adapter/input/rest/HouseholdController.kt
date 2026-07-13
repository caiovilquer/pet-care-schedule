package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.application.adapter.input.security.CurrentHousehold
import dev.vilquer.petcarescheduler.application.adapter.input.security.CurrentJwt
import dev.vilquer.petcarescheduler.application.adapter.input.security.tutorId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.household.*
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.HouseholdContextUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.HouseholdManagementUseCase
import dev.vilquer.petcarescheduler.usecase.result.*
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import jakarta.servlet.http.HttpServletRequest
import dev.vilquer.petcarescheduler.application.service.RateLimitAction
import dev.vilquer.petcarescheduler.application.service.RateLimiterService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class HouseholdInviteRequest(@field:Email @field:Size(max = 255) val email: String, val role: HouseholdRole)
data class HouseholdAcceptRequest(@field:NotBlank @field:Size(min = 32, max = 128) val token: String)
data class HouseholdRoleRequest(val expectedVersion: Long, val role: HouseholdRole)
data class HouseholdRenameRequest(val expectedVersion: Long, @field:NotBlank @field:Size(max = 100) val name: String)
data class HouseholdTimezoneRequest(val expectedVersion: Long, @field:NotBlank @field:Size(max = 64) val timezone: String)
data class HouseholdHandoffRequest(val toTutorId: Long?, @field:NotBlank @field:Size(max = 1_000) val note: String)
data class HouseholdAcceptedResponse(val householdId: UUID)

@RestController
@RequestMapping("/api/v1/households")
@Validated
class HouseholdController(
    private val context: HouseholdContextUseCase,
    private val management: HouseholdManagementUseCase,
    private val current: CurrentHousehold,
    private val rateLimiter: RateLimiterService,
) {
    @GetMapping
    fun list(@AuthenticationPrincipal jwt: CurrentJwt): List<HouseholdSummaryResult> =
        context.list(TutorId(jwt.tutorId()))

    @GetMapping("/current")
    fun overview(@AuthenticationPrincipal jwt: CurrentJwt): HouseholdOverviewResult =
        management.overview(current.resolve(jwt))

    @PutMapping("/{id}/default")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun setDefault(@PathVariable id: UUID, @AuthenticationPrincipal jwt: CurrentJwt) =
        context.setDefault(TutorId(jwt.tutorId()), HouseholdId(id))

    @PatchMapping("/{id}")
    fun rename(
        @PathVariable id: UUID,
        @Valid @RequestBody body: HouseholdRenameRequest,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ) = management.rename(RenameHouseholdCommand(HouseholdId(id), body.expectedVersion, body.name), current.resolve(jwt))

    @PatchMapping("/{id}/timezone")
    fun updateTimezone(
        @PathVariable id: UUID,
        @Valid @RequestBody body: HouseholdTimezoneRequest,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ) = management.updateTimezone(
        UpdateHouseholdTimezoneCommand(HouseholdId(id), body.expectedVersion, body.timezone), current.resolve(jwt),
    )

    @PostMapping("/current/invitations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun invite(
        @Valid @RequestBody body: HouseholdInviteRequest,
        @AuthenticationPrincipal jwt: CurrentJwt,
        request: HttpServletRequest,
    ) {
        val access = current.resolve(jwt)
        rateLimiter.check(
            RateLimitAction.HOUSEHOLD_INVITE,
            "${request.remoteAddr ?: "unknown"}:${access.actorTutorId.value}:${access.householdId.value}",
        )
        management.invite(InviteHouseholdMemberCommand(body.email, body.role), access)
    }

    @DeleteMapping("/current/invitations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeInvitation(@PathVariable id: UUID, @AuthenticationPrincipal jwt: CurrentJwt) =
        management.revokeInvitation(HouseholdInvitationId(id), current.resolve(jwt))

    @PostMapping("/invitations/accept")
    fun accept(@Valid @RequestBody body: HouseholdAcceptRequest, @AuthenticationPrincipal jwt: CurrentJwt) =
        HouseholdAcceptedResponse(management.accept(AcceptHouseholdInvitationCommand(body.token), TutorId(jwt.tutorId())).value)

    @PostMapping("/invitations/preview")
    fun invitationPreview(@Valid @RequestBody body: HouseholdAcceptRequest, @AuthenticationPrincipal jwt: CurrentJwt) =
        management.invitationPreview(AcceptHouseholdInvitationCommand(body.token), TutorId(jwt.tutorId()))

    @PatchMapping("/current/members/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changeRole(
        @PathVariable id: UUID,
        @Valid @RequestBody body: HouseholdRoleRequest,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ) = management.changeRole(ChangeHouseholdMemberRoleCommand(HouseholdMemberId(id), body.expectedVersion, body.role), current.resolve(jwt))

    @DeleteMapping("/current/members/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeMember(@PathVariable id: UUID, @AuthenticationPrincipal jwt: CurrentJwt) =
        management.removeMember(HouseholdMemberId(id), current.resolve(jwt))

    @PostMapping("/current/handoffs")
    @ResponseStatus(HttpStatus.CREATED)
    fun handoff(@Valid @RequestBody body: HouseholdHandoffRequest, @AuthenticationPrincipal jwt: CurrentJwt) =
        management.createHandoff(CreateHouseholdHandoffCommand(body.toTutorId?.let(::TutorId), body.note), current.resolve(jwt))
}
