package dev.vilquer.petcarescheduler.infra.adapter.output.session.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "refresh_token",
    indexes = [
        Index(columnList = "user_id"),
        Index(columnList = "family_id"),
        Index(columnList = "expires_at")
    ]
)
class RefreshTokenJpa(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    var tokenHash: String = "",

    @Column(name = "user_id", nullable = false)
    var userId: Long? = null,

    @Column(name = "family_id", nullable = false)
    var familyId: UUID = UUID.randomUUID(),

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now(),

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "used_at")
    var usedAt: Instant? = null,

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,

    @Column(name = "replaced_by")
    var replacedBy: UUID? = null,

    @Column(name = "user_agent", length = 256)
    var userAgent: String? = null
)
