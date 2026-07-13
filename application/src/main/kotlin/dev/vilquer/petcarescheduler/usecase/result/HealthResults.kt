package dev.vilquer.petcarescheduler.usecase.result

import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurementType
import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurementUnit
import dev.vilquer.petcarescheduler.core.domain.health.HealthRecordType
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class HealthAttachmentResult(
    val id: UUID,
    val mediaAssetId: UUID,
    val filename: String,
    val contentType: String,
    val sizeBytes: Long,
    val contentUrl: String,
)

data class HealthRecordResult(
    val id: UUID,
    val version: Long?,
    val petId: Long,
    val type: HealthRecordType,
    val occurredAt: Instant,
    val title: String,
    val notes: String?,
    val productName: String?,
    val dosage: String?,
    val batchNumber: String?,
    val professionalName: String?,
    val clinicName: String?,
    val costAmount: BigDecimal?,
    val currency: String?,
    val createdByTutorId: Long,
    val attachments: List<HealthAttachmentResult>,
)

data class HealthRecordsPageResult(
    val items: List<HealthRecordResult>,
    val total: Long,
    val page: Int,
    val size: Int,
)

data class HealthMeasurementResult(
    val id: UUID,
    val version: Long?,
    val petId: Long,
    val type: HealthMeasurementType,
    val value: BigDecimal,
    val unit: HealthMeasurementUnit,
    val measuredAt: Instant,
    val notes: String?,
    val createdByTutorId: Long,
)
