package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity

import dev.vilquer.petcarescheduler.core.domain.media.MediaPurpose
import dev.vilquer.petcarescheduler.core.domain.media.MediaStatus
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "media_asset")
class MediaAssetJpa {
    @Id lateinit var id: UUID
    @Version var version: Long? = null
    @Column(name = "tutor_id") var tutorId: Long? = null
    @Column(name = "pet_id") var petId: Long? = null
    @Enumerated(EnumType.STRING) @Column(nullable = false) lateinit var purpose: MediaPurpose
    @Column(name = "original_filename", nullable = false) lateinit var originalFilename: String
    @Column(name = "content_type", nullable = false) lateinit var contentType: String
    @Column(name = "expected_size", nullable = false) var expectedSize: Long = 0
    @Column(name = "checksum_sha256", nullable = false) lateinit var checksumSha256: String
    @Column(name = "staging_key", nullable = false, unique = true) lateinit var stagingKey: String
    @Column(name = "object_key", nullable = false, unique = true) lateinit var objectKey: String
    @Enumerated(EnumType.STRING) @Column(nullable = false) lateinit var status: MediaStatus
    @Column(name = "created_at", nullable = false) lateinit var createdAt: Instant
    @Column(name = "ready_at") var readyAt: Instant? = null
}
