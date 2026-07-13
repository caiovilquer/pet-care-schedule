package dev.vilquer.petcarescheduler.core.domain.household

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@JvmInline value class HouseholdId(val value: UUID)
@JvmInline value class HouseholdMemberId(val value: UUID)
@JvmInline value class HouseholdInvitationId(val value: UUID)

object HouseholdTimezone {
    const val DEFAULT_ID = "America/Sao_Paulo"

    fun parse(value: String?): ZoneId = value?.trim()?.takeIf { it.isNotEmpty() }
        ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
        ?: ZoneId.of(DEFAULT_ID)

    fun requireValid(value: String): ZoneId = try {
        val normalized = value.trim()
        require(normalized in ZoneId.getAvailableZoneIds())
        ZoneId.of(normalized)
    } catch (_: Exception) {
        throw IllegalArgumentException("household_timezone_invalid")
    }
}

enum class HouseholdRole { OWNER, CAREGIVER, VIEWER }

enum class HouseholdPermission {
    VIEW,
    COMPLETE_CARE,
    RECORD_HEALTH,
    MANAGE_PETS,
    MANAGE_PLANS,
    MANAGE_MEMBERS,
    MANAGE_FINANCES,
    SHARE_VETERINARY_SUMMARY,
}

fun HouseholdRole.allows(permission: HouseholdPermission): Boolean = when (this) {
    HouseholdRole.OWNER -> true
    HouseholdRole.CAREGIVER -> permission in setOf(
        HouseholdPermission.VIEW,
        HouseholdPermission.COMPLETE_CARE,
        HouseholdPermission.RECORD_HEALTH,
    )
    HouseholdRole.VIEWER -> permission == HouseholdPermission.VIEW
}

data class HouseholdAccess(
    val householdId: HouseholdId,
    val actorTutorId: TutorId,
    val role: HouseholdRole,
    val zoneId: ZoneId = HouseholdTimezone.parse(null),
) {
    fun can(permission: HouseholdPermission) = role.allows(permission)
}

data class Household(
    val id: HouseholdId = HouseholdId(UUID.randomUUID()),
    val version: Long? = null,
    val name: String,
    val createdByTutorId: TutorId,
    val createdAt: Instant,
    val updatedAt: Instant,
    val timezone: ZoneId = HouseholdTimezone.parse(null),
) {
    init { require(name.isNotBlank() && name.length <= 100) { "household_name_invalid" } }
}

data class HouseholdMember(
    val id: HouseholdMemberId = HouseholdMemberId(UUID.randomUUID()),
    val version: Long? = null,
    val householdId: HouseholdId,
    val tutorId: TutorId,
    val role: HouseholdRole,
    val joinedAt: Instant,
)

data class HouseholdInvitation(
    val id: HouseholdInvitationId = HouseholdInvitationId(UUID.randomUUID()),
    val version: Long? = null,
    val householdId: HouseholdId,
    val email: String,
    val role: HouseholdRole,
    val tokenHash: String,
    val activeKey: String?,
    val invitedByTutorId: TutorId,
    val expiresAt: Instant,
    val acceptedAt: Instant? = null,
    val revokedAt: Instant? = null,
    val createdAt: Instant,
) {
    init {
        require(email.isNotBlank() && email.length <= 255) { "household_invitation_email_invalid" }
        require(tokenHash.matches(Regex("^[a-f0-9]{64}$"))) { "household_invitation_hash_invalid" }
        require(activeKey == null || activeKey.length <= 400) { "household_invitation_active_key_invalid" }
    }

    fun accept(at: Instant) = copy(activeKey = null, acceptedAt = at)
    fun revoke(at: Instant) = copy(activeKey = null, revokedAt = at)
}

enum class HouseholdActivityType {
    MEMBER_JOINED,
    MEMBER_ROLE_CHANGED,
    MEMBER_REMOVED,
    CARE_ASSIGNED,
    CARE_COMPLETED,
    CARE_REOPENED,
    HEALTH_RECORDED,
    HANDOFF,
    ESCALATION_SENT,
}

data class HouseholdActivity(
    val id: UUID = UUID.randomUUID(),
    val householdId: HouseholdId,
    val type: HouseholdActivityType,
    val actorTutorId: TutorId?,
    val targetTutorId: TutorId? = null,
    val petId: PetId? = null,
    val careOccurrenceId: UUID? = null,
    val summary: String,
    val happenedAt: Instant,
) {
    init { require(summary.isNotBlank() && summary.length <= 240) { "household_activity_summary_invalid" } }
}

data class HouseholdHandoff(
    val id: UUID = UUID.randomUUID(),
    val householdId: HouseholdId,
    val fromTutorId: TutorId,
    val toTutorId: TutorId?,
    val note: String,
    val createdAt: Instant,
) {
    init { require(note.isNotBlank() && note.length <= 1_000) { "household_handoff_note_invalid" } }
}
