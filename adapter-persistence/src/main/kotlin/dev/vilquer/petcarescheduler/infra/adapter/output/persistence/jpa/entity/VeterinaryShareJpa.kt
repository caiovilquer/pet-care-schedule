package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "veterinary_share")
class VeterinaryShareJpa {
    @Id lateinit var id: UUID
    @Version var version: Long? = null
    @Column(name = "household_id", nullable = false) lateinit var householdId: UUID
    @Column(name = "pet_id", nullable = false) var petId: Long = 0
    @Column(name = "created_by_tutor_id", nullable = false) var createdByTutorId: Long = 0
    @Column(nullable = false, length = 100) lateinit var label: String
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) lateinit var tokenHash: String
    @Column(name = "period_from", nullable = false) lateinit var periodFrom: LocalDate
    @Column(name = "period_to", nullable = false) lateinit var periodTo: LocalDate
    @Column(name = "include_notes", nullable = false) var includeNotes: Boolean = false
    @Column(name = "include_costs", nullable = false) var includeCosts: Boolean = false
    @Column(name = "include_documents", nullable = false) var includeDocuments: Boolean = false
    @Column(name = "expires_at", nullable = false) lateinit var expiresAt: Instant
    @Column(name = "revoked_at") var revokedAt: Instant? = null
    @Column(name = "created_at", nullable = false) lateinit var createdAt: Instant
    @Column(name = "last_accessed_at") var lastAccessedAt: Instant? = null
    @Column(name = "access_count", nullable = false) var accessCount: Long = 0
}
