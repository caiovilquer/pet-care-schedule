package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.FakeObjectStorage
import dev.vilquer.petcarescheduler.application.FakeTransactionPort
import dev.vilquer.petcarescheduler.application.InMemoryMediaAssetRepo
import dev.vilquer.petcarescheduler.application.InMemoryHealthAttachmentRepo
import dev.vilquer.petcarescheduler.application.InMemoryHealthRecordRepo
import dev.vilquer.petcarescheduler.application.InMemoryPetRepo
import dev.vilquer.petcarescheduler.application.InMemoryTutorRepo
import dev.vilquer.petcarescheduler.application.TEST_HOUSEHOLD_ID
import dev.vilquer.petcarescheduler.application.exception.ForbiddenException
import dev.vilquer.petcarescheduler.application.exception.NotFoundException
import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.media.MediaPurpose
import dev.vilquer.petcarescheduler.core.domain.media.MediaStatus
import dev.vilquer.petcarescheduler.core.domain.health.HealthRecord
import dev.vilquer.petcarescheduler.core.domain.health.HealthRecordType
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.usecase.command.CompleteMediaUploadCommand
import dev.vilquer.petcarescheduler.usecase.command.InitiateMediaUploadCommand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import javax.imageio.ImageIO

class MediaAppServiceTest {
    private val now = Instant.parse("2026-07-12T15:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val tutorId = TutorId(1)
    private val petId = PetId(10)
    private val media = InMemoryMediaAssetRepo()
    private val storage = FakeObjectStorage()
    private val tutors = InMemoryTutorRepo(
        mapOf(
            tutorId to Tutor(
                id = tutorId,
                firstName = "Ana",
                lastName = null,
                email = Email.of("ana@example.com").getOrThrow(),
                passwordHash = "hash",
            ),
        ),
    )
    private val pets = InMemoryPetRepo(
        mapOf(
            petId to Pet(
                id = petId,
                name = "Luna",
                species = "cat",
                breed = null,
                birthdate = null,
                tutorId = tutorId,
            ),
        ),
    )
    private val healthRecords = InMemoryHealthRecordRepo()
    private val healthAttachments = InMemoryHealthAttachmentRepo()
    private val service = MediaAppService(
        media, storage, pets, tutors, healthRecords, healthAttachments,
        FakeTransactionPort(), clock,
    )

    @Test
    fun `valid image is verified promoted and attached to pet`() {
        val image = png()
        val initiated = service.initiate(command(image, petId.value), tutorId)
        val pending = media.findById(initiated.uploadId)!!
        storage.objects[pending.stagingKey] = image

        val result = service.complete(CompleteMediaUploadCommand(initiated.uploadId), tutorId)

        assertEquals(initiated.uploadId, result.id)
        assertEquals(MediaStatus.READY, media.findById(initiated.uploadId)?.status)
        assertEquals(initiated.uploadId, pets.findById(petId)?.photoAssetId)
        assertNotNull(storage.objects[pending.objectKey])
        assertNull(storage.objects[pending.stagingKey])
    }

    @Test
    fun `checksum mismatch never promotes or attaches image`() {
        val expected = png()
        val initiated = service.initiate(command(expected, petId.value), tutorId)
        val pending = media.findById(initiated.uploadId)!!
        storage.objects[pending.stagingKey] = png(rgb = 0x00ff00)

        assertThrows(IllegalArgumentException::class.java) {
            service.complete(CompleteMediaUploadCommand(initiated.uploadId), tutorId)
        }

        assertEquals(MediaStatus.PENDING, media.findById(initiated.uploadId)?.status)
        assertNull(pets.findById(petId)?.photoAssetId)
        assertNull(storage.objects[pending.objectKey])
    }

    @Test
    fun `tutor cannot initiate upload for another tutor pet`() {
        assertThrows(ForbiddenException::class.java) {
            service.initiate(command(png(), petId.value), TutorId(2))
        }
    }

    @Test
    fun `cleanup retains metadata when storage deletion fails for safe retry`() {
        val initiated = service.initiate(command(png(), petId.value), tutorId)
        val pending = media.findById(initiated.uploadId)!!
        media.save(pending.copy(status = MediaStatus.PENDING_DELETE))
        storage.failDeletes = true

        service.cleanupMedia()

        assertNotNull(media.findById(initiated.uploadId))
    }

    @Test
    fun `clinical PDF is attached privately and download requires its owner`() {
        val record = healthRecords.save(
            HealthRecord(
                householdId = TEST_HOUSEHOLD_ID, tutorId = tutorId, petId = petId, type = HealthRecordType.EXAM, occurredAt = now,
                title = "Hemograma", createdByTutorId = tutorId, createdAt = now, updatedAt = now,
            ),
        )
        val pdf = "%PDF-1.7\n1 0 obj\n<<>>\nendobj\n%%EOF".toByteArray()
        val initiated = service.initiate(
            InitiateMediaUploadCommand(
                purpose = MediaPurpose.HEALTH_ATTACHMENT, targetUuid = record.id.value,
                filename = "hemograma.pdf", contentType = "application/pdf", sizeBytes = pdf.size.toLong(),
                checksumSha256 = sha256(pdf),
            ),
            tutorId,
        )
        val pending = media.findById(initiated.uploadId)!!
        storage.objects[pending.stagingKey] = pdf

        val completed = service.complete(CompleteMediaUploadCommand(initiated.uploadId), tutorId)

        assertEquals("/api/v1/health-attachments/${initiated.uploadId}/download-url", completed.contentUrl)
        assertEquals(1, healthAttachments.countByRecord(record.id))
        assertThrows(NotFoundException::class.java) { service.downloadUrl(initiated.uploadId) }
        assertThrows(NotFoundException::class.java) { service.downloadUrl(initiated.uploadId, TutorId(2)) }
        assertNotNull(service.downloadUrl(initiated.uploadId, tutorId))
        assertEquals("hemograma.pdf", storage.lastDownloadFilename)
    }

    @Test
    fun `forged PDF is rejected before promotion`() {
        val record = healthRecords.save(
            HealthRecord(
                householdId = TEST_HOUSEHOLD_ID, tutorId = tutorId, petId = petId, type = HealthRecordType.EXAM, occurredAt = now,
                title = "Exame", createdByTutorId = tutorId, createdAt = now, updatedAt = now,
            ),
        )
        val forged = "%PDF-1.7\nnot-a-complete-document".toByteArray()
        val initiated = service.initiate(
            InitiateMediaUploadCommand(
                purpose = MediaPurpose.HEALTH_ATTACHMENT, targetUuid = record.id.value,
                filename = "exame.pdf", contentType = "application/pdf", sizeBytes = forged.size.toLong(),
                checksumSha256 = sha256(forged),
            ),
            tutorId,
        )
        val pending = media.findById(initiated.uploadId)!!
        storage.objects[pending.stagingKey] = forged

        assertThrows(IllegalArgumentException::class.java) {
            service.complete(CompleteMediaUploadCommand(initiated.uploadId), tutorId)
        }
        assertEquals(MediaStatus.PENDING, media.findById(initiated.uploadId)?.status)
        assertNull(storage.objects[pending.objectKey])
    }

    private fun command(bytes: ByteArray, targetId: Long) = InitiateMediaUploadCommand(
        purpose = MediaPurpose.PET_PHOTO,
        targetId = targetId,
        filename = "luna.png",
        contentType = "image/png",
        sizeBytes = bytes.size.toLong(),
        checksumSha256 = sha256(bytes),
    )

    private fun png(rgb: Int = 0xff00ff): ByteArray = ByteArrayOutputStream().use { output ->
        val image = BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB)
        image.setRGB(0, 0, rgb)
        check(ImageIO.write(image, "png", output))
        output.toByteArray()
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
