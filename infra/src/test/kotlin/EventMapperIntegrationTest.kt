package petcarescheduler.infra.test

import dev.vilquer.petcarescheduler.PetCareSchedulerApplication
import dev.vilquer.petcarescheduler.core.domain.entity.Event
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.Status
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.PetJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.TutorJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.EventMapper
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.EventJpaRepository
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.PetJpaRepository
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.TutorJpaRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@ContextConfiguration(classes = [PetCareSchedulerApplication::class])
class EventMapperIntegrationTest {

    @Autowired
    private lateinit var tutorRepository: TutorJpaRepository

    @Autowired
    private lateinit var petRepository: PetJpaRepository

    @Autowired
    private lateinit var eventRepository: EventJpaRepository

    private lateinit var savedPet: PetJpa
    private val testEventDateTime = LocalDateTime.of(2025, 5, 1, 9, 0)

    @BeforeEach
    fun setUp() {
        // Create and save test tutor
        val tutor = TutorJpa().apply {
            firstName = "Ana"
            email = "ana@ex.com"
            passwordHash = "pwd"
            phoneNumber = "1198888-1111"
        }
        tutorRepository.save(tutor)

        // Create and save test pet
        val pet = PetJpa().apply {
            name = "Luna"
            specie = "Gato"
            race = "Siames"
            birthdate = LocalDate.of(2021, 6, 20)
            tutorId = tutor.id!!
        }
        savedPet = petRepository.save(pet)
    }

    @Test
    fun `should correctly map event from domain to JPA and back to domain`() {
        // Create domain event
        val domainEvent = Event(
            id = null,
            type = EventType.VACCINE,
            description = "Vacina anual",
            dateStart = testEventDateTime,
            recurrence = null,
            status = Status.PENDING,
            petId = PetId(savedPet.id!!)
        )

        // Map domain to JPA and persist
        val eventJpa = EventMapper.toJpa(domainEvent, existing = null)
        val savedJpa = eventRepository.save(eventJpa)

        // Retrieve from database
        val fetchedJpa = eventRepository.findById(savedJpa.id!!).orElseThrow()

        // Map JPA back to domain
        val fetchedDomain = EventMapper.toDomain(fetchedJpa)

        // Assertions
        assertNotNull(fetchedDomain.id)
        assertEquals(savedJpa.id, fetchedDomain.id!!.value)
        assertEquals(EventType.VACCINE, fetchedDomain.type)
        assertEquals("Vacina anual", fetchedDomain.description)
        assertEquals(testEventDateTime, fetchedDomain.dateStart)
        assertNull(fetchedDomain.recurrence)
        assertEquals(Status.PENDING, fetchedDomain.status)
        assertEquals(PetId(savedPet.id!!), fetchedDomain.petId)
    }
}