package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.household.*
import java.time.Instant
import java.util.UUID

data class HouseholdMemberDetails(
    val member: HouseholdMember,
    val firstName: String,
    val lastName: String?,
    val email: String,
    val avatarAssetId: UUID?,
)

data class HouseholdActivityDetails(
    val activity: HouseholdActivity,
    val actorName: String?,
    val targetName: String?,
    val petName: String?,
)

data class HouseholdHandoffDetails(
    val handoff: HouseholdHandoff,
    val fromName: String,
    val toName: String?,
)

interface HouseholdRepositoryPort {
    fun save(household: Household): Household
    fun findById(id: HouseholdId): Household?
    fun findByIdForUpdate(id: HouseholdId): Household?
    fun listForTutor(tutorId: TutorId): List<Pair<Household, HouseholdRole>>
}

interface HouseholdMemberRepositoryPort {
    fun save(member: HouseholdMember): HouseholdMember
    fun findAccess(tutorId: TutorId, householdId: HouseholdId): HouseholdMember?
    fun findAccessForUpdate(tutorId: TutorId, householdId: HouseholdId): HouseholdMember?
    fun findByIdForUpdate(id: HouseholdMemberId, householdId: HouseholdId): HouseholdMember?
    fun listDetails(householdId: HouseholdId): List<HouseholdMemberDetails>
    fun count(householdId: HouseholdId): Long
    fun countOwners(householdId: HouseholdId): Long
    fun delete(id: HouseholdMemberId)
}

interface HouseholdInvitationRepositoryPort {
    fun save(invitation: HouseholdInvitation): HouseholdInvitation
    fun findActiveByHashForUpdate(hash: String): HouseholdInvitation?
    fun findActiveByKeyForUpdate(activeKey: String): HouseholdInvitation?
    fun findByIdForUpdate(id: HouseholdInvitationId, householdId: HouseholdId): HouseholdInvitation?
    fun listActive(householdId: HouseholdId, now: Instant): List<HouseholdInvitation>
}

interface HouseholdActivityRepositoryPort {
    fun save(activity: HouseholdActivity): HouseholdActivity
    fun listRecent(householdId: HouseholdId, limit: Int): List<HouseholdActivityDetails>
}

interface HouseholdHandoffRepositoryPort {
    fun save(handoff: HouseholdHandoff): HouseholdHandoff
    fun listRecent(householdId: HouseholdId, limit: Int): List<HouseholdHandoffDetails>
}

fun interface HouseholdInvitationNotifierPort {
    fun sendInvitation(email: String, householdName: String, inviterName: String, token: String, expiresAt: Instant)
}
