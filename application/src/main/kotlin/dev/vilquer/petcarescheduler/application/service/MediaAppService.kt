package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.exception.ForbiddenException
import dev.vilquer.petcarescheduler.application.exception.NotFoundException
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.media.MediaAsset
import dev.vilquer.petcarescheduler.core.domain.media.MediaPurpose
import dev.vilquer.petcarescheduler.core.domain.media.MediaStatus
import dev.vilquer.petcarescheduler.usecase.command.CompleteMediaUploadCommand
import dev.vilquer.petcarescheduler.usecase.command.DeleteMediaCommand
import dev.vilquer.petcarescheduler.usecase.command.InitiateMediaUploadCommand
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.MediaAssetRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.ObjectStoragePort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PetRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TransactionPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.MediaMaintenanceUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.MediaUploadUseCase
import dev.vilquer.petcarescheduler.usecase.result.MediaAssetResult
import dev.vilquer.petcarescheduler.usecase.result.MediaUploadInitiatedResult
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.imageio.ImageIO

class MediaAppService(
    private val media: MediaAssetRepositoryPort,
    private val storage: ObjectStoragePort,
    private val pets: PetRepositoryPort,
    private val tutors: TutorRepositoryPort,
    private val transaction: TransactionPort,
    private val clock: Clock = Clock.systemUTC(),
) : MediaUploadUseCase, MediaMaintenanceUseCase {

    override fun initiate(command: InitiateMediaUploadCommand, tutorId: TutorId): MediaUploadInitiatedResult {
        validateRequest(command)
        val petId = when (command.purpose) {
            MediaPurpose.PET_PHOTO -> PetId(command.targetId).also {
                if (!pets.existsForTutor(it, tutorId)) throw ForbiddenException("Pet não pertence ao tutor autenticado")
            }
            MediaPurpose.TUTOR_AVATAR -> {
                if (command.targetId != tutorId.value) throw ForbiddenException("Avatar não pertence ao tutor autenticado")
                if (tutors.findById(tutorId) == null) throw NotFoundException("Tutor não encontrado")
                null
            }
        }
        val now = Instant.now(clock)
        val id = UUID.randomUUID()
        val extension = EXTENSIONS.getValue(command.contentType)
        val category = command.purpose.name.lowercase()
        val asset = MediaAsset(
            id = id,
            tutorId = tutorId,
            petId = petId,
            purpose = command.purpose,
            originalFilename = sanitizeFilename(command.filename),
            contentType = command.contentType,
            expectedSize = command.sizeBytes,
            checksumSha256 = command.checksumSha256.lowercase(),
            stagingKey = "staging/${tutorId.value}/$id",
            objectKey = "media/${tutorId.value}/$category/$id.$extension",
            createdAt = now,
        )
        media.save(asset)
        val signed = storage.presignUpload(asset.stagingKey, asset.contentType, asset.checksumSha256, UPLOAD_TTL)
        return MediaUploadInitiatedResult(id, signed.url, signed.headers, now.plus(UPLOAD_TTL))
    }

    override fun complete(command: CompleteMediaUploadCommand, tutorId: TutorId): MediaAssetResult {
        val asset = ownedPending(command.uploadId, tutorId)
        val bytes = storage.readObject(asset.stagingKey, MAX_BYTES + 1)
        validateUploadedImage(asset, bytes)
        storage.promote(asset.stagingKey, asset.objectKey, asset.contentType)
        val now = Instant.now(clock)

        transaction.execute {
            when (asset.purpose) {
                MediaPurpose.PET_PHOTO -> {
                    val pet = pets.findByIdAndTutor(asset.petId!!, tutorId)
                        ?: throw NotFoundException("Pet não encontrado")
                    pet.photoAssetId?.takeIf { it != asset.id }?.let { oldId ->
                        media.findById(oldId)?.let { media.save(it.copy(status = MediaStatus.PENDING_DELETE)) }
                    }
                    pets.save(pet.copy(photoAssetId = asset.id, photoUrl = null))
                }
                MediaPurpose.TUTOR_AVATAR -> {
                    val tutor = tutors.findById(tutorId) ?: throw NotFoundException("Tutor não encontrado")
                    tutor.avatarAssetId?.takeIf { it != asset.id }?.let { oldId ->
                        media.findById(oldId)?.let { media.save(it.copy(status = MediaStatus.PENDING_DELETE)) }
                    }
                    tutors.save(tutor.copy(avatarAssetId = asset.id, avatar = null))
                }
            }
            media.save(asset.copy(status = MediaStatus.READY, readyAt = now))
        }
        return MediaAssetResult(asset.id, contentPath(asset.id))
    }

    override fun delete(command: DeleteMediaCommand, tutorId: TutorId) {
        val asset = media.findById(command.mediaId) ?: return
        if (asset.tutorId != tutorId) throw ForbiddenException("Mídia não pertence ao tutor autenticado")
        transaction.execute {
            when (asset.purpose) {
                MediaPurpose.PET_PHOTO -> asset.petId?.let { id ->
                    pets.findByIdAndTutor(id, tutorId)?.takeIf { it.photoAssetId == asset.id }
                        ?.let { pets.save(it.copy(photoAssetId = null)) }
                }
                MediaPurpose.TUTOR_AVATAR -> tutors.findById(tutorId)?.takeIf { it.avatarAssetId == asset.id }
                    ?.let { tutors.save(it.copy(avatarAssetId = null)) }
            }
            media.save(asset.copy(status = MediaStatus.PENDING_DELETE))
        }
    }

    override fun downloadUrl(mediaId: UUID): String {
        val asset = media.findById(mediaId)?.takeIf { it.status == MediaStatus.READY }
            ?: throw NotFoundException("Mídia não encontrada")
        return storage.presignDownload(asset.objectKey, DOWNLOAD_TTL)
    }

    override fun cleanupMedia() {
        val cutoff = Instant.now(clock).minus(PENDING_RETENTION)
        media.findCleanupCandidates(cutoff, 100).forEach { asset ->
            val removedFromStorage = runCatching {
                storage.delete(asset.stagingKey)
                storage.delete(asset.objectKey)
            }.isSuccess
            if (removedFromStorage) media.delete(asset.id)
        }
    }

    private fun ownedPending(id: UUID, tutorId: TutorId): MediaAsset {
        val asset = media.findById(id) ?: throw NotFoundException("Upload não encontrado")
        if (asset.tutorId != tutorId) throw ForbiddenException("Upload não pertence ao tutor autenticado")
        require(asset.status == MediaStatus.PENDING) { "upload_not_pending" }
        require(asset.createdAt.plus(PENDING_RETENTION).isAfter(Instant.now(clock))) { "upload_expired" }
        return asset
    }

    private fun validateRequest(command: InitiateMediaUploadCommand) {
        require(command.contentType in EXTENSIONS) { "unsupported_image_type" }
        require(command.sizeBytes in 1..MAX_BYTES) { "image_size_invalid" }
        require(command.filename.isNotBlank() && command.filename.length <= 180) { "filename_invalid" }
        require(command.checksumSha256.lowercase().matches(SHA256_REGEX)) { "checksum_invalid" }
    }

    private fun validateUploadedImage(asset: MediaAsset, bytes: ByteArray) {
        require(bytes.size.toLong() == asset.expectedSize) { "uploaded_size_mismatch" }
        val actualHash = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        require(MessageDigest.isEqual(actualHash.toByteArray(), asset.checksumSha256.toByteArray())) {
            "uploaded_checksum_mismatch"
        }
        require(hasExpectedSignature(bytes, asset.contentType)) { "invalid_image_signature" }
        ImageIO.createImageInputStream(bytes.inputStream()).use { stream ->
            val reader = ImageIO.getImageReaders(stream).asSequence().firstOrNull()
                ?: throw IllegalArgumentException("invalid_image_content")
            try {
                reader.input = stream
                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                require(width in 1..MAX_DIMENSION && height in 1..MAX_DIMENSION) { "image_dimensions_invalid" }
                require(width.toLong() * height.toLong() <= MAX_PIXELS) { "image_pixels_invalid" }
                reader.read(0) ?: throw IllegalArgumentException("invalid_image_content")
            } finally {
                reader.dispose()
            }
        }
    }

    private fun hasExpectedSignature(bytes: ByteArray, contentType: String): Boolean = when (contentType) {
        "image/jpeg" -> bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() && bytes[2] == 0xff.toByte()
        "image/png" -> bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(PNG_SIGNATURE)
        else -> false
    }

    private fun sanitizeFilename(filename: String): String = filename
        .replace(Regex("[\\r\\n\\u0000-\\u001f\\u007f]"), "")
        .substringAfterLast('/').substringAfterLast('\\').take(180)

    private fun contentPath(id: UUID) = "/api/v1/media/$id/content"

    companion object {
        const val MAX_BYTES = 5L * 1024 * 1024
        private const val MAX_DIMENSION = 6000
        private const val MAX_PIXELS = 25_000_000L
        private val EXTENSIONS = mapOf("image/jpeg" to "jpg", "image/png" to "png")
        private val SHA256_REGEX = Regex("^[a-f0-9]{64}$")
        private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        private val UPLOAD_TTL = Duration.ofMinutes(3)
        private val DOWNLOAD_TTL = Duration.ofMinutes(15)
        private val PENDING_RETENTION = Duration.ofMinutes(15)
    }
}
