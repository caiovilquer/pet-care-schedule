package dev.vilquer.petcarescheduler.usecase.result

import java.time.Instant
import java.util.UUID

data class MediaUploadInitiatedResult(
    val uploadId: UUID,
    val uploadUrl: String,
    val headers: Map<String, String>,
    val expiresAt: Instant,
)
data class MediaAssetResult(val id: UUID, val contentPath: String)
