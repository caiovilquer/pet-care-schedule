package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository

import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.HealthRecordAttachmentJpa
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

data class HealthAttachmentRow(
    val id: UUID,
    val healthRecordId: UUID,
    val mediaAssetId: UUID,
    val originalFilename: String,
    val contentType: String,
    val sizeBytes: Long,
    val createdAt: Instant,
)

interface HealthRecordAttachmentJpaRepository : JpaRepository<HealthRecordAttachmentJpa, UUID> {
    fun countByHealthRecordId(healthRecordId: UUID): Long
    fun findByMediaAssetId(mediaAssetId: UUID): HealthRecordAttachmentJpa?
    fun deleteByMediaAssetId(mediaAssetId: UUID)

    @Query("""
        select new dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.HealthAttachmentRow(
            a.id, a.healthRecordId, a.mediaAssetId, m.originalFilename, m.contentType, m.expectedSize, a.createdAt
        )
        from HealthRecordAttachmentJpa a, MediaAssetJpa m
        where a.mediaAssetId = m.id and a.healthRecordId in :recordIds
          and m.status = dev.vilquer.petcarescheduler.core.domain.media.MediaStatus.READY
        order by a.createdAt asc, a.id asc
    """)
    fun findDetails(@Param("recordIds") recordIds: Collection<UUID>): List<HealthAttachmentRow>
}
