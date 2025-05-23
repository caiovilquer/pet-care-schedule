package petcarescheduler.infra.test

import dev.vilquer.petcarescheduler.core.domain.entity.Event
import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.Status
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Frequency
import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.EventJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.PetJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.embeddable.RecurrenceEmb
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.PetMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PetMapperTest {
    @Test
    fun `should correctly map JPA entity to domain entity`() {
        // Arrange
        val petJpa = createTestPetJpa()

        // Act
        val domainPet = PetMapper.toDomain(petJpa)

        // Assert
        assertJpaToDomainMapping(petJpa, domainPet)
    }

    @Test
    fun `should correctly map domain entity to JPA entity`() {
        // Arrange
        val domainPet = createTestPetDomain()

        // Act
        val petJpa = PetMapper.toJpa(domainPet)

        // Assert
        assertDomainToJpaMapping(domainPet, petJpa)
    }

    @Test
    fun `should maintain bidirectional mapping integrity`() {
        // Arrange
        val originalJpa = createTestPetJpa()

        // Act
        val domain = PetMapper.toDomain(originalJpa)
        val mappedBackToJpa = PetMapper.toJpa(domain)

        // Assert
        assertJpaEquals(originalJpa, mappedBackToJpa)
    }

    private fun assertJpaToDomainMapping(jpa: PetJpa, domain: Pet) {
        // Verify base pet properties
        assertEquals(jpa.id, domain.id?.value, "Pet ID should be correctly mapped")
        assertEquals(jpa.name, domain.name, "Pet name should be correctly mapped")
        assertEquals(jpa.specie, domain.specie, "Pet species should be correctly mapped")
        assertEquals(jpa.race, domain.race, "Pet race should be correctly mapped")
        assertEquals(jpa.birthdate, domain.birthdate, "Pet birthdate should be correctly mapped")
        assertEquals(jpa.tutorId, domain.tutorId.value, "Pet tutor ID should be correctly mapped")

        // Verify events mapping
        assertEquals(jpa.events.size, domain.events.size, "Number of events should match")

        // Match events by ID for comparison
        jpa.events.forEach { jpaEvent ->
            val matchingDomainEvent = domain.events.find { it.id?.value == jpaEvent.id }

            if (matchingDomainEvent != null) {
                assertEquals(jpaEvent.id, matchingDomainEvent.id?.value, "Event ID should be correctly mapped")
                assertEquals(
                    jpaEvent.type.name,
                    matchingDomainEvent.type.name,
                    "Event type should be correctly mapped"
                )
                assertEquals(
                    jpaEvent.description,
                    matchingDomainEvent.description,
                    "Event description should be correctly mapped"
                )
                assertEquals(
                    jpaEvent.dateStart,
                    matchingDomainEvent.dateStart,
                    "Event start date should be correctly mapped"
                )
                assertEquals(
                    jpaEvent.recurrenceEmb?.finalDate, matchingDomainEvent.recurrence?.finalDate,
                    "Event recurrence final date should be correctly mapped"
                )
                assertEquals(
                    jpaEvent.status.name,
                    matchingDomainEvent.status.name,
                    "Event status should be correctly mapped"
                )
                assertEquals(jpa.id, matchingDomainEvent.petId.value, "Event pet ID should be correctly mapped")
            } else {
                throw AssertionError("Missing matching domain event for JPA event with ID: ${jpaEvent.id}")
            }
        }
    }

    private fun assertDomainToJpaMapping(domain: Pet, jpa: PetJpa) {
        // Verify base pet properties
        assertEquals(domain.id?.value, jpa.id, "Pet ID should be correctly mapped")
        assertEquals(domain.name, jpa.name, "Pet name should be correctly mapped")
        assertEquals(domain.specie, jpa.specie, "Pet species should be correctly mapped")
        assertEquals(domain.race, jpa.race, "Pet race should be correctly mapped")
        assertEquals(domain.birthdate, jpa.birthdate, "Pet birthdate should be correctly mapped")
        assertEquals(domain.tutorId.value, jpa.tutorId, "Pet tutor ID should be correctly mapped")

        // Verify events mapping
        assertEquals(domain.events.size, jpa.events.size, "Number of events should match")

        // Match events by ID for comparison
        domain.events.forEach { domainEvent ->
            val matchingJpaEvent = jpa.events.find { it.id == domainEvent.id?.value }

            if (matchingJpaEvent != null) {
                assertEquals(domainEvent.id?.value, matchingJpaEvent.id, "Event ID should be correctly mapped")
                assertEquals(
                    domainEvent.type.name,
                    matchingJpaEvent.type.name,
                    "Event type should be correctly mapped"
                )
                assertEquals(
                    domainEvent.description,
                    matchingJpaEvent.description,
                    "Event description should be correctly mapped"
                )
                assertEquals(
                    domainEvent.dateStart,
                    matchingJpaEvent.dateStart,
                    "Event start date should be correctly mapped"
                )
                assertEquals(
                    domainEvent.recurrence?.finalDate, matchingJpaEvent.recurrenceEmb?.finalDate,
                    "Event recurrence final date should be correctly mapped"
                )
                assertEquals(
                    domainEvent.status.name,
                    matchingJpaEvent.status.name,
                    "Event status should be correctly mapped"
                )
                assertEquals(domainEvent.petId.value, jpa.id, "Event pet ID reference should be correctly mapped")
            } else {
                throw AssertionError("Missing matching JPA event for domain event with ID: ${domainEvent.id?.value}")
            }
        }
    }

    private fun assertJpaEquals(expected: PetJpa, actual: PetJpa) {
        assertEquals(expected.id, actual.id, "Pet ID should match")
        assertEquals(expected.name, actual.name, "Pet name should match")
        assertEquals(expected.specie, actual.specie, "Pet species should match")
        assertEquals(expected.race, actual.race, "Pet race should match")
        assertEquals(expected.birthdate, actual.birthdate, "Pet birthdate should match")
        assertEquals(expected.tutorId, actual.tutorId, "Pet tutor ID should match")

        // Compare events
        assertEquals(expected.events.size, actual.events.size, "Number of events should match")

        // Sort events by ID for consistent comparison
        val sortedExpectedEvents = expected.events.sortedBy { it.id }
        val sortedActualEvents = actual.events.sortedBy { it.id }

        sortedExpectedEvents.forEachIndexed { index, expectedEvent ->
            val actualEvent = sortedActualEvents[index]
            assertEquals(expectedEvent.id, actualEvent.id, "Event ID should match")
            assertEquals(expectedEvent.type, actualEvent.type, "Event type should match")
            assertEquals(expectedEvent.description, actualEvent.description, "Event description should match")
            assertEquals(expectedEvent.dateStart, actualEvent.dateStart, "Event start date should match")
            assertEquals(
                expectedEvent.recurrenceEmb?.finalDate, actualEvent.recurrenceEmb?.finalDate,
                "Event recurrence final date should match"
            )
            assertEquals(expectedEvent.status, actualEvent.status, "Event status should match")
        }
    }

    // Test data creation methods
    private fun createTestPetJpa(): PetJpa {
        val petJpa = PetJpa().apply {
            id = 42L
            name = "Bidu"
            specie = "Cachorro"
            race = "SRD"
            birthdate = LocalDate.of(2020, 1, 15)
            tutorId = 7L
            events = mutableListOf()
        }

        // Add events to the pet
        val event1 = EventJpa().apply {
            id = 1L
            type = EventType.MEDICINE
            description = "Consulta de rotina"
            dateStart = LocalDateTime.of(2023, 6, 15, 10, 0)
            recurrenceEmb = RecurrenceEmb(frequency = Frequency.DAILY).apply {
                finalDate = LocalDateTime.of(2023, 6, 15, 11, 0)
            }
            status = Status.PENDING
            petId = petJpa.id!!
        }

        val event2 = EventJpa().apply {
            id = 2L
            type = EventType.VACCINE
            description = "Vacina anual"
            dateStart = LocalDateTime.of(2023, 7, 20, 14, 30)
            recurrenceEmb = RecurrenceEmb(frequency = Frequency.MONTHLY).apply {
                finalDate = LocalDateTime.of(2023, 7, 20, 15, 0)
            }
            status = Status.PENDING
            petId = petJpa.id!!
        }

        petJpa.events.add(event1)
        petJpa.events.add(event2)

        return petJpa
    }

    private fun createTestPetDomain(): Pet {
        val petId = PetId(42L)
        val tutorId = TutorId(7L)

        val event1 = Event(
            id = EventId(1L),
            type = EventType.MEDICINE,
            description = "Consulta de rotina",
            dateStart = LocalDateTime.of(2023, 6, 15, 10, 0),
            recurrence = Recurrence(Frequency.WEEKLY, finalDate = LocalDateTime.of(2023, 6, 15, 11, 0)),
            status = Status.PENDING,
            petId = petId
        )

        val event2 = Event(
            id = EventId(2L),
            type = EventType.VACCINE,
            description = "Vacina anual",
            dateStart = LocalDateTime.of(2023, 7, 20, 14, 30),
            recurrence = Recurrence(frequency = Frequency.YEARLY, finalDate = LocalDateTime.of(2023, 7, 20, 15, 0)),
            status = Status.PENDING,
            petId = petId
        )

        return Pet(
            id = petId,
            name = "Bidu",
            specie = "Cachorro",
            race = "SRD",
            birthdate = LocalDate.of(2020, 1, 15),
            tutorId = tutorId,
            events = listOf(event1, event2)
        )
    }
}