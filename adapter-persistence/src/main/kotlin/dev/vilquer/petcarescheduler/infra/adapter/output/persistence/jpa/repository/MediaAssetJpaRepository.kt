package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository

import dev.vilquer.petcarescheduler.core.domain.media.MediaStatus
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.MediaAssetJpa
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface MediaAssetJpaRepository : JpaRepository<MediaAssetJpa, UUID> {
    @Query("""
        select m from MediaAssetJpa m
        where m.status = :deleteStatus
           or (m.status = :pendingStatus and m.createdAt < :pendingBefore)
           or m.tutorId is null
           or (m.purpose = dev.vilquer.petcarescheduler.core.domain.media.MediaPurpose.PET_PHOTO and m.petId is null)
        order by m.createdAt asc
    """)
    fun findCleanupCandidates(
        deleteStatus: MediaStatus,
        pendingStatus: MediaStatus,
        pendingBefore: Instant,
        pageable: Pageable,
    ): List<MediaAssetJpa>

    @Modifying
    @Query("update MediaAssetJpa m set m.status = :status where m.petId = :petId")
    fun markPetAssets(petId: Long, status: MediaStatus)

    @Modifying
    @Query("update MediaAssetJpa m set m.status = :status where m.tutorId = :tutorId")
    fun markTutorAssets(tutorId: Long, status: MediaStatus)
}
