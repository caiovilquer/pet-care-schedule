package petcarescheduler.infra.test

import dev.vilquer.petcarescheduler.PetCareSchedulerApplication
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.TutorMapper
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.TutorJpaRepository
import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate

@DataJpaTest
@ContextConfiguration(classes = [PetCareSchedulerApplication::class])
class TutorMapperIntegrationTest {

    @Autowired
    private lateinit var tutorRepository: TutorJpaRepository

    @Test
    fun `should correctly map Tutor and Pets between domain and persistence layers`() {
        // Arrange
        val originalTutor = createTestTutorWithPet()

        // Act
        val persistedTutor = persistAndRetrieveTutor(originalTutor)

        // Assert
        assertTutorMappedCorrectly(originalTutor, persistedTutor)
        assertPetsMappedCorrectly(persistedTutor)
    }

    private fun createTestTutorWithPet(): Tutor {
        return Tutor(
            id = TutorId(1),
            firstName = "Carlos",
            lastName = "Mendes",
            email = Email.of("carlos@ex.com").getOrThrow(),
            passwordHash = "hash123",
            phoneNumber = PhoneNumber.of("1199999-0000").getOrNull(),
            avatar = null,
            pets = listOf(
                Pet(
                    id = null,
                    name = "Rex",
                    specie = "Cachorro",
                    race = "SRD",
                    birthdate = LocalDate.of(2020, 5, 10),
                    tutorId = TutorId(5) // will be adjusted by mapper
                )
            )
        )
    }

    private fun persistAndRetrieveTutor(domainTutor: Tutor): Tutor {
        // Convert domain to JPA entity
        val tutorJpa = TutorMapper.toJpa(domainTutor)

        // Persist to database
        val savedJpa = tutorRepository.save(tutorJpa)

        // Retrieve from database
        val fetchedJpa = tutorRepository.findById(savedJpa.id!!).orElseThrow()

        // Convert back to domain
        return TutorMapper.toDomain(fetchedJpa)
    }

    private fun assertTutorMappedCorrectly(original: Tutor, persisted: Tutor) {
        assertNotNull(persisted.id, "Tutor should receive an ID after saving")
        assertEquals(original.firstName, persisted.firstName)
        assertEquals(original.lastName, persisted.lastName)
        assertEquals(original.email, persisted.email)
        assertEquals(original.passwordHash, persisted.passwordHash)
        assertEquals(original.phoneNumber, persisted.phoneNumber)
    }

    private fun assertPetsMappedCorrectly(tutor: Tutor) {
        assertEquals(1, tutor.pets.size)

        val pet = tutor.pets.first()
        assertNotNull(pet.id, "Pet should receive an ID after saving")
        assertEquals("Rex", pet.name)
        assertEquals("Cachorro", pet.specie)
        assertEquals("SRD", pet.race)
        assertEquals(LocalDate.of(2020, 5, 10), pet.birthdate)

        // Pet's tutorId should match the tutor's id
        assertEquals(tutor.id, pet.tutorId)
    }
}