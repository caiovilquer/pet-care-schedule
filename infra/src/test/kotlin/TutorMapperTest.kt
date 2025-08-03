package petcarescheduler.infra.test

import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.PetJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.TutorJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.TutorMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TutorMapperTest {

    // Test data constants
    private companion object {
        const val TUTOR_ID = 10L
        const val PET_ID = 88L
        val SAMPLE_BIRTHDATE: LocalDate = LocalDate.of(2021, 6, 15)
    }

    @Test
    fun `should correctly map TutorJpa to domain entity with nested pets`() {
        // Arrange
        val tutorJpa = createSampleTutorJpa()

        // Act
        val domain = TutorMapper.toDomain(tutorJpa)

        // Assert
        with(domain) {
            // Tutor assertions
            assertEquals(TutorId(TUTOR_ID), id)
            assertEquals("Carlos", firstName)
            assertEquals("Silva", lastName)
            assertEquals("carlos@ex.com", email.toString())
            assertEquals("abc123", passwordHash)
            assertEquals(PhoneNumber.of("55599990000").getOrNull(), phoneNumber)
            assertEquals("avatar.png", avatar)

            // Pets assertions
            assertEquals(1, pets.size)
            with(pets[0]) {
                assertEquals(PetId(PET_ID), id)
                assertEquals("Luna", name)
                assertEquals("Gato", specie)
                assertEquals("Siames", race)
                assertEquals(SAMPLE_BIRTHDATE, birthdate)
                assertEquals(TutorId(TUTOR_ID), tutorId)
            }
        }
    }

    @Test
    fun `should create new TutorJpa instance when mapping from domain without existing entity`() {
        // Arrange
        val domain = createNewTutorDomain()

        // Act
        val jpa = TutorMapper.toJpa(domain)

        // Assert
        with(jpa) {
            assertNull(id)
            assertEquals("Mariana", firstName)
            assertEquals("Costa", lastName)
            assertEquals("mari@ex.com", email)
            assertEquals("senhaX", passwordHash)
            assertEquals(PhoneNumber.of("55588881111").getOrNull(), phoneNumber)
            assertNull(avatar)

            // Pets assertions
            assertEquals(1, pets.size)
            with(pets.first()) {
                assertNull(id)
                assertEquals("Rex", name)
                assertEquals("Cachorro", specie)
                assertEquals("Labrador", race)
                assertEquals(LocalDate.of(2018, 2, 1), birthdate)
            }
        }
    }

    @Test
    fun `should update existing TutorJpa when mapping from domain with existing entity`() {
        // Arrange
        val existing = createExistingTutorJpa()
        val domain = createUpdatedTutorDomain()

        // Act
        val jpa = TutorMapper.toJpa(domain, existing)

        // Assert
        with(jpa) {
            // Basic fields assertions
            assertEquals(5L, id)
            assertEquals("Ana Paula", firstName)
            assertEquals("Lima", lastName)
            assertEquals("ana.paula@ex.com", email)
            assertEquals("newHash", passwordHash)
            assertEquals(PhoneNumber.of("55577773333").getOrNull(), phoneNumber)
            assertEquals("new.png", avatar)

            // Pets assertions
            assertEquals(2, pets.size)

            // Verify updated pet
            val updatedPet = pets.find { it.id == 50L }
            assertNotNull(updatedPet)
            assertEquals("Bola Nova", updatedPet?.name)

            // Verify added pet
            val addedPet = pets.find { it.id == null }
            assertNotNull(addedPet)
            assertEquals("Nina", addedPet?.name)
            assertEquals("Gato", addedPet?.specie)
        }
    }

    // Helper methods for creating test data
    private fun createSampleTutorJpa(): TutorJpa {
        val tutorJpa = TutorJpa().apply {
            id = TUTOR_ID
            firstName = "Carlos"
            lastName = "Silva"
            email = "carlos@ex.com"
            passwordHash = "abc123"
            phoneNumber = "55599990000"
            avatar = "avatar.png"
        }

        val petJpa = PetJpa().apply {
            id = PET_ID
            name = "Luna"
            specie = "Gato"
            race = "Siames"
            birthdate = SAMPLE_BIRTHDATE
        }

        tutorJpa.pets.add(petJpa)
        return tutorJpa
    }

    private fun createNewTutorDomain(): Tutor {
        return Tutor(
            id = null,
            firstName = "Mariana",
            lastName = "Costa",
            email = Email.of("mari@ex.com").getOrThrow(),
            passwordHash = "senhaX",
            phoneNumber = PhoneNumber.of("55588881111").getOrNull(),
            avatar = null,
            pets = listOf(
                Pet(
                    id = null,
                    name = "Rex",
                    specie = "Cachorro",
                    race = "Labrador",
                    birthdate = LocalDate.of(2018, 2, 1),
                    tutorId = TutorId(10)
                )
            )
        )
    }

    private fun createExistingTutorJpa(): TutorJpa {
        return TutorJpa().apply {
            id = 5
            firstName = "Ana"
            lastName = "Lima"
            email = "ana@ex.com"
            phoneNumber = "55577772222"
            avatar = "old.png"

            pets.add(PetJpa().apply {
                id = 50
                name = "Bola"
                specie = "Papagaio"
                race = null
                birthdate = LocalDate.of(2017, 3, 3)
            })
        }
    }

    private fun createUpdatedTutorDomain(): Tutor {
        return Tutor(
            id = TutorId(5),
            firstName = "Ana Paula",
            lastName = "Lima",
            email = Email.of("ana.paula@ex.com").getOrThrow(),
            passwordHash = "newHash",
            phoneNumber = PhoneNumber.of("55577773333").getOrNull(),
            avatar = "new.png",
            pets = listOf(
                Pet(
                    id = PetId(50),
                    name = "Bola Nova",
                    specie = "Papagaio",
                    race = null,
                    birthdate = LocalDate.of(2017, 3, 3),
                    tutorId = TutorId(5)
                ),
                Pet(
                    id = null,
                    name = "Nina",
                    specie = "Gato",
                    race = "SRD",
                    birthdate = LocalDate.of(2020, 12, 12),
                    tutorId = TutorId(5)
                )
            )
        )
    }
}