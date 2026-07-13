package dev.vilquer.petcarescheduler.usecase.result

import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurementType
import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurementUnit
import dev.vilquer.petcarescheduler.core.domain.health.HealthRecordType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class VeterinaryPetResult(
    val id: Long,
    val name: String,
    val species: String,
    val breed: String?,
    val birthdate: LocalDate?,
)

data class CareAdherenceResult(
    val completed: Int,
    val overdue: Int,
    val upcoming: Int,
    val cancelled: Int,
    val adherencePercent: BigDecimal?,
)

data class VeterinaryRecordResult(
    val id: UUID,
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
)

data class VeterinaryMeasurementResult(
    val id: UUID,
    val type: HealthMeasurementType,
    val value: BigDecimal,
    val unit: HealthMeasurementUnit,
    val measuredAt: Instant,
)

data class VeterinaryDocumentResult(
    val mediaId: UUID,
    val recordId: UUID,
    val filename: String,
    val contentType: String,
    val sizeBytes: Long,
    val occurredAt: Instant,
)

data class VeterinarySummaryResult(
    val pet: VeterinaryPetResult,
    val from: LocalDate,
    val to: LocalDate,
    val generatedAt: Instant,
    val adherence: CareAdherenceResult,
    val records: List<VeterinaryRecordResult>,
    val measurements: List<VeterinaryMeasurementResult>,
    val documents: List<VeterinaryDocumentResult>,
    val truncated: Boolean,
)

data class VeterinaryShareCreatedResult(val id: UUID, val token: String, val expiresAt: Instant)
data class VeterinaryShareResult(
    val id: UUID,
    val version: Long?,
    val petId: Long,
    val label: String,
    val from: LocalDate,
    val to: LocalDate,
    val includeNotes: Boolean,
    val includeCosts: Boolean,
    val includeDocuments: Boolean,
    val expiresAt: Instant,
    val revokedAt: Instant?,
    val createdAt: Instant,
    val lastAccessedAt: Instant?,
    val accessCount: Long,
)
data class PublicVeterinarySummaryResult(
    val shareId: UUID,
    val label: String,
    val expiresAt: Instant,
    val summary: VeterinarySummaryResult,
)
