package dev.vilquer.petcarescheduler.core.domain.media

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import java.time.Instant
import java.util.UUID

enum class MediaPurpose { PET_PHOTO, TUTOR_AVATAR }
enum class MediaStatus { PENDING, READY, PENDING_DELETE, REJECTED }

data class MediaAsset(
    val id: UUID = UUID.randomUUID(),
    val version: Long? = null,
    val tutorId: TutorId?,
    val petId: PetId?,
    val purpose: MediaPurpose,
    val originalFilename: String,
    val contentType: String,
    val expectedSize: Long,
    val checksumSha256: String,
    val stagingKey: String,
    val objectKey: String,
    val status: MediaStatus = MediaStatus.PENDING,
    val createdAt: Instant,
    val readyAt: Instant? = null,
) {
    init {
        require(expectedSize > 0) { "expectedSize must be positive" }
        require(checksumSha256.matches(Regex("^[a-f0-9]{64}$"))) { "invalid SHA-256" }
    }
}
