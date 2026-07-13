package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "health_record_attachment")
class HealthRecordAttachmentJpa {
    @Id lateinit var id: UUID
    @Column(name = "health_record_id", nullable = false) lateinit var healthRecordId: UUID
    @Column(name = "media_asset_id", nullable = false) lateinit var mediaAssetId: UUID
    @Column(name = "created_at", nullable = false) lateinit var createdAt: Instant
}
