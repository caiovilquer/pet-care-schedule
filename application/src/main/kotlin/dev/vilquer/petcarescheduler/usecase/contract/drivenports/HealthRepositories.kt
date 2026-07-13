package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId
import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurement
import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurementId
import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurementType
import dev.vilquer.petcarescheduler.core.domain.health.HealthRecord
import dev.vilquer.petcarescheduler.core.domain.health.HealthRecordAttachment
import dev.vilquer.petcarescheduler.core.domain.health.HealthRecordId
import dev.vilquer.petcarescheduler.core.domain.health.HealthRecordType
import java.time.Instant
import java.util.UUID

data class HealthRecordFilter(
    val petId: PetId,
    val from: Instant,
    val to: Instant,
    val type: HealthRecordType?,
)

data class HealthAttachmentDetails(
    val id: UUID,
    val healthRecordId: HealthRecordId,
    val mediaAssetId: UUID,
    val originalFilename: String,
    val contentType: String,
    val sizeBytes: Long,
    val createdAt: Instant,
)

interface HealthRecordRepositoryPort {
    fun save(record: HealthRecord): HealthRecord
    fun findByIdAndTutor(id: HealthRecordId, tutorId: TutorId): HealthRecord?
    fun findByIdAndTutorForUpdate(id: HealthRecordId, tutorId: TutorId): HealthRecord?
    fun search(tutorId: TutorId, filter: HealthRecordFilter, page: Int, size: Int): List<HealthRecord>
    fun count(tutorId: TutorId, filter: HealthRecordFilter): Long
    fun delete(id: HealthRecordId)
    fun findByIdAndHousehold(id: HealthRecordId, householdId: HouseholdId): HealthRecord?
    fun findByIdAndHouseholdForUpdate(id: HealthRecordId, householdId: HouseholdId): HealthRecord?
    fun searchByHousehold(householdId: HouseholdId, filter: HealthRecordFilter, page: Int, size: Int): List<HealthRecord>
    fun countByHousehold(householdId: HouseholdId, filter: HealthRecordFilter): Long
}

interface HealthMeasurementRepositoryPort {
    fun save(measurement: HealthMeasurement): HealthMeasurement
    fun findByIdAndTutor(id: HealthMeasurementId, tutorId: TutorId): HealthMeasurement?
    fun findByIdAndTutorForUpdate(id: HealthMeasurementId, tutorId: TutorId): HealthMeasurement?
    fun list(
        tutorId: TutorId,
        petId: PetId,
        type: HealthMeasurementType?,
        from: Instant,
        to: Instant,
        limit: Int,
    ): List<HealthMeasurement>
    fun delete(id: HealthMeasurementId)
    fun findByIdAndHouseholdForUpdate(id: HealthMeasurementId, householdId: HouseholdId): HealthMeasurement?
    fun listByHousehold(householdId: HouseholdId, petId: PetId, type: HealthMeasurementType?, from: Instant, to: Instant, limit: Int): List<HealthMeasurement>
}

interface HealthRecordAttachmentRepositoryPort {
    fun save(attachment: HealthRecordAttachment): HealthRecordAttachment
    fun countByRecord(recordId: HealthRecordId): Long
    fun listByRecordIds(recordIds: Collection<HealthRecordId>): Map<HealthRecordId, List<HealthAttachmentDetails>>
    fun findByMediaId(mediaId: UUID): HealthRecordAttachment?
    fun deleteByMediaId(mediaId: UUID)
}
