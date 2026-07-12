package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import java.time.Duration

data class PresignedUpload(val url: String, val headers: Map<String, String>)

interface ObjectStoragePort {
    fun presignUpload(key: String, contentType: String, checksumSha256: String, expiresIn: Duration): PresignedUpload
    fun readObject(key: String, maxBytes: Long): ByteArray
    fun promote(sourceKey: String, destinationKey: String, contentType: String)
    fun delete(key: String)
    fun presignDownload(key: String, expiresIn: Duration): String
}
