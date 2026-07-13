package dev.vilquer.petcarescheduler.core.domain.health

import java.time.Instant
import java.util.UUID

data class HealthRecordAttachment(
    val id: UUID = UUID.randomUUID(),
    val healthRecordId: HealthRecordId,
    val mediaAssetId: UUID,
    val createdAt: Instant,
)
