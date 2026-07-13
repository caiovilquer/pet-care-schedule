package dev.vilquer.petcarescheduler.infra.adapter.output.external

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId
import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurement
import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurementId
import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurementType
import dev.vilquer.petcarescheduler.core.domain.health.HealthRecord
import dev.vilquer.petcarescheduler.core.domain.health.HealthRecordAttachment
import dev.vilquer.petcarescheduler.core.domain.health.HealthRecordId
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.HealthRecordAttachmentJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.toDomain
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.toJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.HealthMeasurementJpaRepository
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.HealthRecordAttachmentJpaRepository
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.HealthRecordJpaRepository
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.HealthAttachmentDetails
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.HealthMeasurementRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.HealthRecordAttachmentRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.HealthRecordFilter
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.HealthRecordRepositoryPort
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Repository
class HealthRecordRepositoryAdapter(private val jpa: HealthRecordJpaRepository) : HealthRecordRepositoryPort {
    // A versão só é incrementada no flush; a API precisa devolver o valor real
    // para que a próxima edição use um token de concorrência válido.
    override fun save(record: HealthRecord) = jpa.saveAndFlush(record.toJpa()).toDomain()
    override fun findByIdAndTutor(id: HealthRecordId, tutorId: TutorId) = jpa.findOwned(id.value, tutorId.value)?.toDomain()
    override fun findByIdAndTutorForUpdate(id: HealthRecordId, tutorId: TutorId) = jpa.findOwnedForUpdate(id.value, tutorId.value)?.toDomain()
    override fun search(tutorId: TutorId, filter: HealthRecordFilter, page: Int, size: Int) =
        jpa.search(tutorId.value, filter.petId.value, filter.from, filter.to, filter.type, PageRequest.of(page, size))
            .content.map { it.toDomain() }
    override fun count(tutorId: TutorId, filter: HealthRecordFilter) =
        jpa.search(tutorId.value, filter.petId.value, filter.from, filter.to, filter.type, PageRequest.of(0, 1)).totalElements
    override fun delete(id: HealthRecordId) = jpa.deleteById(id.value)
    override fun findByIdAndHousehold(id: HealthRecordId, householdId: HouseholdId) = jpa.findByIdAndHouseholdId(id.value, householdId.value)?.toDomain()
    override fun findByIdAndHouseholdForUpdate(id: HealthRecordId, householdId: HouseholdId) = jpa.findByHouseholdForUpdate(id.value, householdId.value)?.toDomain()
    override fun searchByHousehold(householdId: HouseholdId, filter: HealthRecordFilter, page: Int, size: Int) =
        jpa.searchByHousehold(householdId.value, filter.petId.value, filter.from, filter.to, filter.type, PageRequest.of(page, size)).content.map { it.toDomain() }
    override fun countByHousehold(householdId: HouseholdId, filter: HealthRecordFilter) =
        jpa.searchByHousehold(householdId.value, filter.petId.value, filter.from, filter.to, filter.type, PageRequest.of(0, 1)).totalElements
    override fun searchCostsByHousehold(householdId: HouseholdId, petId: PetId?, from: Instant, to: Instant, limit: Int) =
        jpa.findCostsByHousehold(householdId.value, petId?.value, from, to, PageRequest.of(0, limit)).map { it.toDomain() }
}

@Repository
class HealthMeasurementRepositoryAdapter(private val jpa: HealthMeasurementJpaRepository) : HealthMeasurementRepositoryPort {
    override fun save(measurement: HealthMeasurement) = jpa.saveAndFlush(measurement.toJpa()).toDomain()
    override fun findByIdAndTutor(id: HealthMeasurementId, tutorId: TutorId) = jpa.findOwned(id.value, tutorId.value)?.toDomain()
    override fun findByIdAndTutorForUpdate(id: HealthMeasurementId, tutorId: TutorId) =
        jpa.findOwnedForUpdate(id.value, tutorId.value)?.toDomain()
    override fun list(
        tutorId: TutorId,
        petId: PetId,
        type: HealthMeasurementType?,
        from: Instant,
        to: Instant,
        limit: Int,
    ) = jpa.findSeries(tutorId.value, petId.value, type, from, to, PageRequest.of(0, limit)).map { it.toDomain() }
    override fun delete(id: HealthMeasurementId) = jpa.deleteById(id.value)
    override fun findByIdAndHouseholdForUpdate(id: HealthMeasurementId, householdId: HouseholdId) =
        jpa.findByHouseholdForUpdate(id.value, householdId.value)?.toDomain()
    override fun listByHousehold(householdId: HouseholdId, petId: PetId, type: HealthMeasurementType?, from: Instant, to: Instant, limit: Int) =
        jpa.findSeriesByHousehold(householdId.value, petId.value, type, from, to, PageRequest.of(0, limit)).map { it.toDomain() }
}

@Repository
class HealthRecordAttachmentRepositoryAdapter(
    private val jpa: HealthRecordAttachmentJpaRepository,
) : HealthRecordAttachmentRepositoryPort {
    override fun save(attachment: HealthRecordAttachment): HealthRecordAttachment {
        jpa.save(HealthRecordAttachmentJpa().also {
            it.id = attachment.id; it.healthRecordId = attachment.healthRecordId.value
            it.mediaAssetId = attachment.mediaAssetId; it.createdAt = attachment.createdAt
        })
        return attachment
    }
    override fun countByRecord(recordId: HealthRecordId) = jpa.countByHealthRecordId(recordId.value)
    override fun listByRecordIds(recordIds: Collection<HealthRecordId>): Map<HealthRecordId, List<HealthAttachmentDetails>> {
        if (recordIds.isEmpty()) return emptyMap()
        return jpa.findDetails(recordIds.map { it.value }).map { row ->
            HealthAttachmentDetails(
                row.id, HealthRecordId(row.healthRecordId), row.mediaAssetId, row.originalFilename,
                row.contentType, row.sizeBytes, row.createdAt,
            )
        }.groupBy { it.healthRecordId }
    }
    override fun findByMediaId(mediaId: UUID) = jpa.findByMediaAssetId(mediaId)?.let {
        HealthRecordAttachment(it.id, HealthRecordId(it.healthRecordId), it.mediaAssetId, it.createdAt)
    }
    @Transactional
    override fun deleteByMediaId(mediaId: UUID) = jpa.deleteByMediaAssetId(mediaId)
}
