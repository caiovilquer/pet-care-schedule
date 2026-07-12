package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.media.MediaAsset
import java.time.Instant
import java.util.UUID

interface MediaAssetRepositoryPort {
    fun save(asset: MediaAsset): MediaAsset
    fun findById(id: UUID): MediaAsset?
    fun delete(id: UUID)
    fun findCleanupCandidates(pendingBefore: Instant, limit: Int): List<MediaAsset>
    fun markPetAssetsForDeletion(petId: PetId)
    fun markTutorAssetsForDeletion(tutorId: TutorId)
}
