package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.FakeObjectStorage
import dev.vilquer.petcarescheduler.application.FakeTransactionPort
import dev.vilquer.petcarescheduler.application.InMemoryMediaAssetRepo
import dev.vilquer.petcarescheduler.application.InMemoryPetRepo
import dev.vilquer.petcarescheduler.application.InMemoryTutorRepo
import dev.vilquer.petcarescheduler.application.exception.ForbiddenException
import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.media.MediaPurpose
import dev.vilquer.petcarescheduler.core.domain.media.MediaStatus
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
    private val service = MediaAppService(media, storage, pets, tutors, FakeTransactionPort(), clock)

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
