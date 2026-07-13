package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity

import dev.vilquer.petcarescheduler.core.domain.household.HouseholdActivityType
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdRole
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity @Table(name = "household")
class HouseholdJpa {
    @Id lateinit var id: UUID
    @Version var version: Long? = null
    @Column(nullable = false, length = 100) lateinit var name: String
    @Column(name = "created_by_tutor_id", nullable = false) var createdByTutorId: Long = 0
    @Column(name = "created_at", nullable = false) lateinit var createdAt: Instant
    @Column(name = "updated_at", nullable = false) lateinit var updatedAt: Instant
}

@Entity @Table(name = "household_member")
class HouseholdMemberJpa {
    @Id lateinit var id: UUID
    @Version var version: Long? = null
    @Column(name = "household_id", nullable = false) lateinit var householdId: UUID
    @Column(name = "tutor_id", nullable = false) var tutorId: Long = 0
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) lateinit var role: HouseholdRole
    @Column(name = "joined_at", nullable = false) lateinit var joinedAt: Instant
}

@Entity @Table(name = "household_invitation")
class HouseholdInvitationJpa {
    @Id lateinit var id: UUID
    @Version var version: Long? = null
    @Column(name = "household_id", nullable = false) lateinit var householdId: UUID
    @Column(nullable = false, length = 255) lateinit var email: String
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) lateinit var role: HouseholdRole
    @Column(name = "token_hash", nullable = false, length = 64) lateinit var tokenHash: String
    @Column(name = "active_key", length = 400) var activeKey: String? = null
    @Column(name = "invited_by_tutor_id", nullable = false) var invitedByTutorId: Long = 0
    @Column(name = "expires_at", nullable = false) lateinit var expiresAt: Instant
    @Column(name = "accepted_at") var acceptedAt: Instant? = null
    @Column(name = "revoked_at") var revokedAt: Instant? = null
    @Column(name = "created_at", nullable = false) lateinit var createdAt: Instant
}

@Entity @Table(name = "household_activity")
class HouseholdActivityJpa {
    @Id lateinit var id: UUID
    @Column(name = "household_id", nullable = false) lateinit var householdId: UUID
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) lateinit var type: HouseholdActivityType
    @Column(name = "actor_tutor_id") var actorTutorId: Long? = null
    @Column(name = "target_tutor_id") var targetTutorId: Long? = null
    @Column(name = "pet_id") var petId: Long? = null
    @Column(name = "care_occurrence_id") var careOccurrenceId: UUID? = null
    @Column(nullable = false, length = 240) lateinit var summary: String
    @Column(name = "happened_at", nullable = false) lateinit var happenedAt: Instant
}

@Entity @Table(name = "household_handoff")
class HouseholdHandoffJpa {
    @Id lateinit var id: UUID
    @Column(name = "household_id", nullable = false) lateinit var householdId: UUID
    @Column(name = "from_tutor_id", nullable = false) var fromTutorId: Long = 0
    @Column(name = "to_tutor_id") var toTutorId: Long? = null
    @Column(nullable = false, length = 1000) lateinit var note: String
    @Column(name = "created_at", nullable = false) lateinit var createdAt: Instant
}
