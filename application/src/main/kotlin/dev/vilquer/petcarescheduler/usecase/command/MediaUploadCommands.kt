package dev.vilquer.petcarescheduler.usecase.command

import dev.vilquer.petcarescheduler.core.domain.media.MediaPurpose
import java.util.UUID

data class InitiateMediaUploadCommand(
    val purpose: MediaPurpose,
    val targetId: Long? = null,
    val targetUuid: UUID? = null,
    val filename: String,
    val contentType: String,
    val sizeBytes: Long,
    val checksumSha256: String,
)
data class CompleteMediaUploadCommand(val uploadId: UUID)
data class DeleteMediaCommand(val mediaId: UUID)
