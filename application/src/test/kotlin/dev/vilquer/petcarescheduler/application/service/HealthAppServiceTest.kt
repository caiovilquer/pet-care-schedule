package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.FakeClock
import dev.vilquer.petcarescheduler.application.FakeTransactionPort
import dev.vilquer.petcarescheduler.application.InMemoryHealthAttachmentRepo
import dev.vilquer.petcarescheduler.application.InMemoryHealthMeasurementRepo
import dev.vilquer.petcarescheduler.application.InMemoryHealthRecordRepo
import dev.vilquer.petcarescheduler.application.InMemoryMediaAssetRepo
import dev.vilquer.petcarescheduler.application.InMemoryPetRepo
import dev.vilquer.petcarescheduler.application.exception.ConflictException
import dev.vilquer.petcarescheduler.application.exception.NotFoundException
import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.health.HealthMeasurementType
import dev.vilquer.petcarescheduler.core.domain.health.HealthRecordType
import dev.vilquer.petcarescheduler.usecase.command.CreateHealthMeasurementCommand
import dev.vilquer.petcarescheduler.usecase.command.CreateHealthRecordCommand
import dev.vilquer.petcarescheduler.usecase.command.UpdateHealthRecordCommand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZoneOffset
import java.time.ZonedDateTime

class HealthAppServiceTest {
    private val now = ZonedDateTime.of(2026, 7, 12, 18, 0, 0, 0, ZoneOffset.UTC)
    private val clock = FakeClock(now)
    private val tutorId = TutorId(1)
    private val petId = PetId(10)
    private val otherTutor = TutorId(2)
    private val records = InMemoryHealthRecordRepo()
    private val measurements = InMemoryHealthMeasurementRepo()
    private val service = HealthAppService(
        records, measurements, InMemoryHealthAttachmentRepo(), InMemoryMediaAssetRepo(),
        InMemoryPetRepo(
            mapOf(
                petId to Pet(
                    id = petId, name = "Luna", species = "Cat", breed = null,
                    birthdate = null, tutorId = tutorId,
                ),
            ),
        ),
        FakeTransactionPort(), clock,
    )

    @Test
    fun `record is normalized and stale updates are rejected`() {
        val created = service.create(recordCommand(), tutorId)
        assertEquals("Consulta anual", created.title)
        assertEquals(0, created.version)

        val updated = service.update(
            UpdateHealthRecordCommand(
                recordId = dev.vilquer.petcarescheduler.core.domain.health.HealthRecordId(created.id),
                expectedVersion = created.version!!, type = HealthRecordType.CONSULTATION,
                occurredAt = now.minusHours(1).toInstant(), title = "Retorno", notes = null,
                productName = null, dosage = null, batchNumber = null,
                professionalName = "Dra. Ana", clinicName = null,
                costAmount = BigDecimal("100.00"), currency = "brl",
            ),
            tutorId,
        )
        assertEquals(1, updated.version)
        assertEquals("BRL", updated.currency)

        assertThrows(ConflictException::class.java) {
            service.update(
                UpdateHealthRecordCommand(
                    dev.vilquer.petcarescheduler.core.domain.health.HealthRecordId(created.id), created.version!!,
                    HealthRecordType.CONSULTATION, now.minusHours(1).toInstant(), "Edição velha", null,
                    null, null, null, null, null, null, null,
                ),
                tutorId,
            )
        }
    }

    @Test
    fun `ownership is hidden and future dates are rejected`() {
        assertThrows(NotFoundException::class.java) { service.create(recordCommand(), otherTutor) }
        assertThrows(IllegalArgumentException::class.java) {
            service.create(recordCommand().copy(occurredAt = now.plusMinutes(6).toInstant()), tutorId)
        }
    }

    @Test
    fun `measurement rules prevent unsafe or incoherent values`() {
        val weight = service.create(
            CreateHealthMeasurementCommand(
                petId, HealthMeasurementType.WEIGHT, BigDecimal("4.75"), now.minusMinutes(2).toInstant(), "Em jejum",
            ),
            tutorId,
        )
        assertEquals("4.75", weight.value.stripTrailingZeros().toPlainString())

        assertThrows(IllegalArgumentException::class.java) {
            service.create(
                CreateHealthMeasurementCommand(
                    petId, HealthMeasurementType.BODY_CONDITION_SCORE,
                    BigDecimal("7.5"), now.minusMinutes(1).toInstant(), null,
                ),
                tutorId,
            )
        }
    }

    private fun recordCommand() = CreateHealthRecordCommand(
        petId = petId, type = HealthRecordType.CONSULTATION, occurredAt = now.minusHours(1).toInstant(),
        title = "  Consulta anual  ", notes = "  Tudo bem  ", productName = null, dosage = null,
        batchNumber = null, professionalName = " Dra. Ana ", clinicName = " Clínica ",
        costAmount = BigDecimal("120.50"), currency = "brl",
    )
}
