package dev.vilquer.petcarescheduler.usecase.command

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdMemberId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdRole

data class InviteHouseholdMemberCommand(val email: String, val role: HouseholdRole)
data class AcceptHouseholdInvitationCommand(val token: String)
data class ChangeHouseholdMemberRoleCommand(
    val memberId: HouseholdMemberId,
    val expectedVersion: Long,
    val role: HouseholdRole,
)
data class CreateHouseholdHandoffCommand(val toTutorId: TutorId?, val note: String)
data class RenameHouseholdCommand(val householdId: HouseholdId, val expectedVersion: Long, val name: String)
data class UpdateHouseholdTimezoneCommand(val householdId: HouseholdId, val expectedVersion: Long, val timezone: String)
