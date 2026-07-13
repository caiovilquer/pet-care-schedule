package dev.vilquer.petcarescheduler.usecase.command

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurementId
import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurementType
import dev.vilquer.petcarescheduler.core.domain.health.HealthRecordId
import dev.vilquer.petcarescheduler.core.domain.health.HealthRecordType
import java.math.BigDecimal
import java.time.Instant

data class CreateHealthRecordCommand(
    val petId: PetId,
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

data class UpdateHealthRecordCommand(
    val recordId: HealthRecordId,
    val expectedVersion: Long,
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

data class SearchHealthRecordsQuery(
    val petId: PetId,
    val from: Instant,
    val to: Instant,
    val type: HealthRecordType? = null,
    val page: Int = 0,
    val size: Int = 20,
)

data class CreateHealthMeasurementCommand(
    val petId: PetId,
    val type: HealthMeasurementType,
    val value: BigDecimal,
    val measuredAt: Instant,
    val notes: String?,
)

data class UpdateHealthMeasurementCommand(
    val measurementId: HealthMeasurementId,
    val expectedVersion: Long,
    val value: BigDecimal,
    val measuredAt: Instant,
    val notes: String?,
)
