package dev.vilquer.petcarescheduler.infra.adapter.output.external

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.household.*
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.*
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.*
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.*
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class HouseholdRepositoryAdapter(private val jpa: HouseholdJpaRepository) : HouseholdRepositoryPort {
    override fun save(household: Household) = jpa.saveAndFlush(household.toJpa()).toDomain()
    override fun findById(id: HouseholdId) = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findByIdForUpdate(id: HouseholdId) = jpa.findForUpdate(id.value)?.toDomain()
    override fun listForTutor(tutorId: TutorId) = jpa.listForTutor(tutorId.value).map { it.household.toDomain() to it.role }
}

@Repository
class HouseholdMemberRepositoryAdapter(private val jpa: HouseholdMemberJpaRepository) : HouseholdMemberRepositoryPort {
    override fun save(member: HouseholdMember) = jpa.saveAndFlush(member.toJpa()).toDomain()
    override fun findAccess(tutorId: TutorId, householdId: HouseholdId) =
        jpa.findByTutorIdAndHouseholdId(tutorId.value, householdId.value)?.toDomain()
    override fun findAccessForUpdate(tutorId: TutorId, householdId: HouseholdId) =
        jpa.findAccessForUpdate(tutorId.value, householdId.value)?.toDomain()
    override fun findByIdForUpdate(id: HouseholdMemberId, householdId: HouseholdId) =
        jpa.findOwnedForUpdate(id.value, householdId.value)?.toDomain()
    override fun listDetails(householdId: HouseholdId) = jpa.listDetails(householdId.value).map {
        HouseholdMemberDetails(
            it.member.toDomain(), it.tutor.firstName, it.tutor.lastName, it.tutor.email, it.tutor.avatarAssetId,
        )
    }
    override fun count(householdId: HouseholdId) = jpa.countByHouseholdId(householdId.value)
    override fun countOwners(householdId: HouseholdId) = jpa.countByHouseholdIdAndRole(householdId.value, HouseholdRole.OWNER)
    override fun delete(id: HouseholdMemberId) = jpa.deleteById(id.value)
}

@Repository
class HouseholdInvitationRepositoryAdapter(private val jpa: HouseholdInvitationJpaRepository) : HouseholdInvitationRepositoryPort {
    override fun save(invitation: HouseholdInvitation) = jpa.saveAndFlush(invitation.toJpa()).toDomain()
    override fun findActiveByHashForUpdate(hash: String) = jpa.findActiveByHashForUpdate(hash)?.toDomain()
    override fun findActiveByKeyForUpdate(activeKey: String) = jpa.findActiveByKeyForUpdate(activeKey)?.toDomain()
    override fun findByIdForUpdate(id: HouseholdInvitationId, householdId: HouseholdId) =
        jpa.findOwnedForUpdate(id.value, householdId.value)?.toDomain()
    override fun listActive(householdId: HouseholdId, now: java.time.Instant) =
        jpa.listActive(householdId.value, now).map { it.toDomain() }
}

@Repository
class HouseholdActivityRepositoryAdapter(private val jpa: HouseholdActivityJpaRepository) : HouseholdActivityRepositoryPort {
    override fun save(activity: HouseholdActivity): HouseholdActivity {
        jpa.save(activity.toJpa()); return activity
    }
    override fun listRecent(householdId: HouseholdId, limit: Int) =
        jpa.listRecent(householdId.value, PageRequest.of(0, limit)).map {
            HouseholdActivityDetails(
                it.activity.toDomain(), name(it.actorFirst, it.actorLast), name(it.targetFirst, it.targetLast), it.petName,
            )
        }
}

@Repository
class HouseholdHandoffRepositoryAdapter(private val jpa: HouseholdHandoffJpaRepository) : HouseholdHandoffRepositoryPort {
    override fun save(handoff: HouseholdHandoff): HouseholdHandoff { jpa.save(handoff.toJpa()); return handoff }
    override fun listRecent(householdId: HouseholdId, limit: Int) =
        jpa.listRecent(householdId.value, PageRequest.of(0, limit)).map {
            HouseholdHandoffDetails(it.handoff.toDomain(), name(it.fromFirst, it.fromLast)!!, name(it.toFirst, it.toLast))
        }
}

private fun name(first: String?, last: String?) = first?.let { listOfNotNull(it, last).joinToString(" ") }
private fun Household.toJpa() = HouseholdJpa().also { j ->
    j.id = id.value; j.version = version; j.name = name; j.createdByTutorId = createdByTutorId.value
    j.createdAt = createdAt; j.updatedAt = updatedAt
}
private fun HouseholdJpa.toDomain() = Household(HouseholdId(id), version, name, TutorId(createdByTutorId), createdAt, updatedAt)
private fun HouseholdMember.toJpa() = HouseholdMemberJpa().also { j ->
    j.id = id.value; j.version = version; j.householdId = householdId.value; j.tutorId = tutorId.value; j.role = role; j.joinedAt = joinedAt
}
private fun HouseholdMemberJpa.toDomain() = HouseholdMember(HouseholdMemberId(id), version, HouseholdId(householdId), TutorId(tutorId), role, joinedAt)
private fun HouseholdInvitation.toJpa() = HouseholdInvitationJpa().also { j ->
    j.id = id.value; j.version = version; j.householdId = householdId.value; j.email = email; j.role = role
    j.tokenHash = tokenHash; j.activeKey = activeKey; j.invitedByTutorId = invitedByTutorId.value
    j.expiresAt = expiresAt; j.acceptedAt = acceptedAt; j.revokedAt = revokedAt; j.createdAt = createdAt
}
private fun HouseholdInvitationJpa.toDomain() = HouseholdInvitation(
    HouseholdInvitationId(id), version, HouseholdId(householdId), email, role, tokenHash, activeKey,
    TutorId(invitedByTutorId), expiresAt, acceptedAt, revokedAt, createdAt,
)
private fun HouseholdActivity.toJpa() = HouseholdActivityJpa().also { j ->
    j.id = id; j.householdId = householdId.value; j.type = type; j.actorTutorId = actorTutorId?.value
    j.targetTutorId = targetTutorId?.value; j.petId = petId?.value; j.careOccurrenceId = careOccurrenceId
    j.summary = summary; j.happenedAt = happenedAt
}
private fun HouseholdActivityJpa.toDomain() = HouseholdActivity(
    id, HouseholdId(householdId), type, actorTutorId?.let(::TutorId), targetTutorId?.let(::TutorId),
    petId?.let(::PetId), careOccurrenceId, summary, happenedAt,
)
private fun HouseholdHandoff.toJpa() = HouseholdHandoffJpa().also { j ->
    j.id = id; j.householdId = householdId.value; j.fromTutorId = fromTutorId.value
    j.toTutorId = toTutorId?.value; j.note = note; j.createdAt = createdAt
}
private fun HouseholdHandoffJpa.toDomain() = HouseholdHandoff(
    id, HouseholdId(householdId), TutorId(fromTutorId), toTutorId?.let(::TutorId), note, createdAt,
)
