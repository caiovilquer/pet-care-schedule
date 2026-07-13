package dev.vilquer.petcarescheduler.usecase.result

import dev.vilquer.petcarescheduler.core.domain.household.HouseholdActivityType
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdRole
import java.time.Instant
import java.util.UUID

data class HouseholdSummaryResult(
    val id: UUID,
    val version: Long?,
    val name: String,
    val role: HouseholdRole,
    val isDefault: Boolean,
    val memberCount: Long,
)

data class HouseholdMemberResult(
    val id: UUID,
    val version: Long?,
    val tutorId: Long,
    val firstName: String,
    val lastName: String?,
    val email: String,
    val avatarAssetId: UUID?,
    val role: HouseholdRole,
    val joinedAt: Instant,
)

data class HouseholdInvitationResult(
    val id: UUID,
    val email: String,
    val role: HouseholdRole,
    val expiresAt: Instant,
    val createdAt: Instant,
)

data class HouseholdActivityResult(
    val id: UUID,
    val type: HouseholdActivityType,
    val actorName: String?,
    val targetName: String?,
    val petName: String?,
    val summary: String,
    val happenedAt: Instant,
)

data class HouseholdHandoffResult(
    val id: UUID,
    val fromName: String,
    val toName: String?,
    val note: String,
    val createdAt: Instant,
)

data class HouseholdOverviewResult(
    val household: HouseholdSummaryResult,
    val members: List<HouseholdMemberResult>,
    val pendingInvitations: List<HouseholdInvitationResult>,
    val recentActivity: List<HouseholdActivityResult>,
    val recentHandoffs: List<HouseholdHandoffResult>,
)
