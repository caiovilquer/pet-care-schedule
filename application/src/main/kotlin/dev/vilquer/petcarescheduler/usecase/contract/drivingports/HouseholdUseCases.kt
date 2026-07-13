package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdAccess
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdInvitationId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdMemberId
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.result.HouseholdOverviewResult
import dev.vilquer.petcarescheduler.usecase.result.HouseholdSummaryResult

interface HouseholdContextUseCase {
    fun resolve(actorTutorId: TutorId, requestedHouseholdId: HouseholdId? = null): HouseholdAccess
    fun list(actorTutorId: TutorId): List<HouseholdSummaryResult>
    fun setDefault(actorTutorId: TutorId, householdId: HouseholdId)
}

interface HouseholdManagementUseCase {
    fun provisionFor(tutorId: TutorId, firstName: String): HouseholdId
    fun overview(access: HouseholdAccess): HouseholdOverviewResult
    fun invite(command: InviteHouseholdMemberCommand, access: HouseholdAccess)
    fun accept(command: AcceptHouseholdInvitationCommand, actorTutorId: TutorId): HouseholdId
    fun revokeInvitation(id: HouseholdInvitationId, access: HouseholdAccess)
    fun changeRole(command: ChangeHouseholdMemberRoleCommand, access: HouseholdAccess)
    fun removeMember(memberId: HouseholdMemberId, access: HouseholdAccess)
    fun createHandoff(command: CreateHouseholdHandoffCommand, access: HouseholdAccess)
    fun rename(command: RenameHouseholdCommand, access: HouseholdAccess): HouseholdSummaryResult
}
