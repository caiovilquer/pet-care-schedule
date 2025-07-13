package petcarescheduler.infra.test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.EventJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.embeddable.RecurrenceEmb
import dev.vilquer.petcarescheduler.core.domain.entity.Event
import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.core.domain.entity.Status
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import dev.vilquer.petcarescheduler.core.domain.valueobject.Frequency
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.EventMapper

class EventMapperTest {

    // Common test constants
    companion object {
        private const val TEST_PET_ID = 123L
        private const val TEST_EVENT_ID = 99L
        private const val VACCINE_DESCRIPTION = "Vacinar contra raiva"
        private const val MEDICINE_DESCRIPTION = "Dar comprimido"
        private const val SERVICE_DESCRIPTION = "Tosa e banho"
    }

    @Test
    fun `toDomain should correctly map all fields`() {
        // Arrange
        val startDate = LocalDateTime.of(2025, 1, 10, 14, 30)
        val jpaEntity = createVaccineEventJpa(startDate)

        // Act
        val domainEntity = EventMapper.toDomain(jpaEntity)

        // Assert
        assertDomainEntityMatchesJpa(domainEntity, jpaEntity)
    }

    @Test
    fun `toJpa without existing entity should create new instance with all fields`() {
        // Arrange
        val startDate = LocalDateTime.of(2025, 2, 20, 9, 0)
        val domainEntity = createMedicineEventDomain(startDate)

        // Act
        val jpaEntity = EventMapper.toJpa(domainEntity)

        // Assert
        assertJpaEntityMatchesDomain(jpaEntity, domainEntity)
    }

    @Test
    fun `toJpa with existing entity should update fields while preserving id`() {
        // Arrange
        val existingJpa = createServiceEventJpa(LocalDateTime.of(2025, 3, 1, 10, 0))
        val newStartDate = LocalDateTime.of(2025, 3, 5, 11, 0)
        val updatedDomain = createUpdatedServiceEventDomain(newStartDate, existingJpa.id!!)

        // Act
        val resultJpa = EventMapper.toJpa(updatedDomain, existingJpa)

        // Assert
        assertJpaEntityMatchesDomain(resultJpa, updatedDomain)
        assertEquals(existingJpa.id, resultJpa.id, "ID should be preserved from existing entity")
    }

    private fun createVaccineEventJpa(startDate: LocalDateTime): EventJpa {
        val recurrence = RecurrenceEmb(
            frequency = Frequency.WEEKLY,
            intervalCount = 2,
            repetitions = 5,
            finalDate = startDate.plusDays(10)
        )

        return EventJpa().apply {
            id = TEST_EVENT_ID
            type = EventType.VACCINE
            description = VACCINE_DESCRIPTION
            dateStart = startDate
            recurrenceEmb = recurrence
            status = Status.PENDING
            petId = TEST_PET_ID
        }
    }

    private fun createMedicineEventDomain(startDate: LocalDateTime): Event {
        val recurrence = Recurrence(
            frequency = Frequency.DAILY,
            intervalCount = 1,
            repetitions = null,
            finalDate = null
        )

        return Event(
            id = null,
            type = EventType.MEDICINE,
            description = MEDICINE_DESCRIPTION,
            dateStart = startDate,
            recurrence = recurrence,
            status = Status.DONE,
            petId = PetId(TEST_PET_ID)
        )
    }

    private fun createServiceEventJpa(startDate: LocalDateTime): EventJpa {
        return EventJpa().apply {
            id = TEST_EVENT_ID
            type = EventType.SERVICE
            description = SERVICE_DESCRIPTION
            dateStart = startDate
            recurrenceEmb = null
            status = Status.PENDING
            petId = TEST_PET_ID
        }
    }

    private fun createUpdatedServiceEventDomain(startDate: LocalDateTime, id: Long): Event {
        return Event(
            id = EventId(id),
            type = EventType.SERVICE,
            description = "$SERVICE_DESCRIPTION (updated)",
            dateStart = startDate,
            recurrence = null,
            status = Status.DONE,
            petId = PetId(TEST_PET_ID)
        )
    }

    private fun assertDomainEntityMatchesJpa(domain: Event, jpa: EventJpa) {
        assertEquals(jpa.id?.let { EventId(it) }, domain.id, "ID should match")
        assertEquals(jpa.type, domain.type, "Type should match")
        assertEquals(jpa.description, domain.description, "Description should match")
        assertEquals(jpa.dateStart, domain.dateStart, "Start date should match")
        assertEquals(jpa.status, domain.status, "Status should match")
        assertEquals(PetId(jpa.petId), domain.petId, "Pet ID should match")

        // Check recurrence mapping
        if (jpa.recurrenceEmb == null) {
            assertNull(domain.recurrence, "Recurrence should be null")
        } else {
            assertNotNull(domain.recurrence, "Recurrence should not be null")
            assertEquals(jpa.recurrenceEmb?.frequency, domain.recurrence?.frequency, "Frequency should match")
            assertEquals(
                jpa.recurrenceEmb?.intervalCount,
                domain.recurrence?.intervalCount,
                "Interval count should match"
            )
            assertEquals(jpa.recurrenceEmb?.repetitions, domain.recurrence?.repetitions, "Repetitions should match")
            assertEquals(jpa.recurrenceEmb?.finalDate, domain.recurrence?.finalDate, "Final date should match")
        }
    }

    private fun assertJpaEntityMatchesDomain(jpa: EventJpa, domain: Event) {
        if (domain.id != null) {
            assertEquals(domain.id!!.value, jpa.id, "ID should match")
        }
        assertEquals(domain.type, jpa.type, "Type should match")
        assertEquals(domain.description, jpa.description, "Description should match")
        assertEquals(domain.dateStart, jpa.dateStart, "Start date should match")
        assertEquals(domain.status, jpa.status, "Status should match")
        assertEquals(domain.petId.value, jpa.petId, "Pet ID should match")

        // Check recurrence mapping
        if (domain.recurrence == null) {
            assertNull(jpa.recurrenceEmb, "Recurrence should be null")
        } else {
            assertNotNull(jpa.recurrenceEmb, "Recurrence should not be null")
            assertEquals(domain.recurrence!!.frequency, jpa.recurrenceEmb?.frequency, "Frequency should match")
            assertEquals(
                domain.recurrence!!.intervalCount,
                jpa.recurrenceEmb?.intervalCount,
                "Interval count should match"
            )
            assertEquals(domain.recurrence!!.repetitions, jpa.recurrenceEmb?.repetitions, "Repetitions should match")
            assertEquals(domain.recurrence!!.finalDate, jpa.recurrenceEmb?.finalDate, "Final date should match")
        }
    }
}