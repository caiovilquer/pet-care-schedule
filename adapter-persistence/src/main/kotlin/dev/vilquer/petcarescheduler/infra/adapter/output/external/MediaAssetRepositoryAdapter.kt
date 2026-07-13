package dev.vilquer.petcarescheduler.infra.adapter.output.external

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.media.MediaAsset
import dev.vilquer.petcarescheduler.core.domain.media.MediaStatus
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.MediaAssetJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.MediaAssetJpaRepository
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.MediaAssetRepositoryPort
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Repository
class MediaAssetRepositoryAdapter(private val repository: MediaAssetJpaRepository) : MediaAssetRepositoryPort {
    override fun save(asset: MediaAsset): MediaAsset = repository.save(asset.toJpa()).toDomain()
    override fun findById(id: UUID): MediaAsset? = repository.findById(id).orElse(null)?.toDomain()
    override fun findByIdForUpdate(id: UUID): MediaAsset? = repository.findByIdForUpdate(id)?.toDomain()
    override fun delete(id: UUID) = repository.deleteById(id)
    override fun findCleanupCandidates(pendingBefore: Instant, limit: Int): List<MediaAsset> =
        repository.findCleanupCandidates(MediaStatus.PENDING_DELETE, MediaStatus.PENDING, pendingBefore, PageRequest.of(0, limit))
            .map { it.toDomain() }

    @Transactional
    override fun markPetAssetsForDeletion(petId: PetId) = repository.markPetAssets(petId.value, MediaStatus.PENDING_DELETE).let { }
    @Transactional
    override fun markTutorAssetsForDeletion(tutorId: TutorId) = repository.markTutorAssets(tutorId.value, MediaStatus.PENDING_DELETE).let { }

    private fun MediaAsset.toJpa() = MediaAssetJpa().also {
        it.id = id; it.version = version; it.tutorId = tutorId?.value; it.householdId = householdId?.value; it.petId = petId?.value; it.healthRecordId = healthRecordId
        it.purpose = purpose; it.originalFilename = originalFilename; it.contentType = contentType
        it.expectedSize = expectedSize; it.checksumSha256 = checksumSha256; it.stagingKey = stagingKey
        it.objectKey = objectKey; it.status = status; it.createdAt = createdAt; it.readyAt = readyAt
    }

    private fun MediaAssetJpa.toDomain() = MediaAsset(
        id = id, version = version, tutorId = tutorId?.let(::TutorId), householdId = householdId?.let(::HouseholdId), petId = petId?.let(::PetId), healthRecordId = healthRecordId,
        purpose = purpose, originalFilename = originalFilename, contentType = contentType,
        expectedSize = expectedSize, checksumSha256 = checksumSha256, stagingKey = stagingKey,
        objectKey = objectKey, status = status, createdAt = createdAt, readyAt = readyAt,
    )
}
