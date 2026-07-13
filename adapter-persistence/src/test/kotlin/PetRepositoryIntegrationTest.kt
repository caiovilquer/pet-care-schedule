package petcarescheduler.infra.test

import dev.vilquer.petcarescheduler.infra.AbstractPostgresIntegrationTest
import dev.vilquer.petcarescheduler.infra.PersistenceTestApplication
import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.TutorJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.HouseholdJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.HouseholdJpaRepository
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.PetMapper
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.PetJpaRepository
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.TutorJpaRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.Instant
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = [PersistenceTestApplication::class])
class PetRepositoryIntegrationTest : AbstractPostgresIntegrationTest() {

    @Autowired
    lateinit var tutorRepoJpa: TutorJpaRepository

    @Autowired
    lateinit var petRepoJpa: PetJpaRepository
    @Autowired lateinit var householdRepoJpa: HouseholdJpaRepository

    private fun householdFor(tutor: TutorJpa): HouseholdId {
        val id = UUID.randomUUID()
        householdRepoJpa.save(HouseholdJpa().also { it.id = id; it.name = "Família teste"; it.createdByTutorId = tutor.id!!; it.createdAt = Instant.now(); it.updatedAt = Instant.now() })
        return HouseholdId(id)
    }

    @Test
    fun `should persist and retrieve pet entity with correct mapping`() {
        // 1) Cria e persiste um tutor
        val tutorJpa = TutorJpa().apply {
            firstName = "Ana"
            email = "ana@ex.com"
            passwordHash = "hash"
            phoneNumber = "1198888-1111"
        }
        tutorRepoJpa.save(tutorJpa)

        // 2) Cria domínio Pet e converte para JPA
        val petDom = Pet(
            id = null,
            name = "Rex",
            species = "Cachorro",
            breed = "Labrador",
            birthdate = LocalDate.of(2019, 8, 1),
            photoUrl = "https://example.com/pets/rex.png",
            tutorId = TutorId(tutorJpa.id!!), householdId = householdFor(tutorJpa)
        )
        val petJpa = PetMapper.toJpa(petDom, existing = null)

        // 3) Persiste via Spring Data
        val savedJpa = petRepoJpa.save(petJpa)

        // 4) Busca de volta e converte para domínio
        val fetchedJpa = petRepoJpa.findById(savedJpa.id!!).get()
        val fetchedDom = PetMapper.toDomain(fetchedJpa)

        // 5) Asserções
        assertNotNull(fetchedDom.id)
        assertEquals("Rex", fetchedDom.name)
        assertEquals("Cachorro", fetchedDom.species)
        assertEquals("Labrador", fetchedDom.breed)
        assertEquals(LocalDate.of(2019, 8, 1), fetchedDom.birthdate)
        assertEquals("https://example.com/pets/rex.png", fetchedDom.photoUrl)
        assertEquals(TutorId(tutorJpa.id!!), fetchedDom.tutorId)
    }

    @Test
    fun `should update pet entity and reflect changes in domain`() {
        // Persistir tutor inicial
        val tutorJpa = TutorJpa().apply {
            firstName = "Ana"
            email = "ana@ex.com"
            passwordHash = "hash"
            phoneNumber = "1198888-1111"
        }
        tutorRepoJpa.save(tutorJpa)

        // Persistir pet
        val originalDom = Pet(
            id = null,
            name = "Rex",
            species = "Cachorro",
            breed = "Labrador",
            birthdate = LocalDate.of(2019, 8, 1),
            photoUrl = "https://example.com/pets/rex.png",
            tutorId = TutorId(tutorJpa.id!!), householdId = householdFor(tutorJpa)
        )
        val originalJpa = PetMapper.toJpa(originalDom)
        val savedJpa = petRepoJpa.save(originalJpa)

        // Atualizar domínio
        val updatedDom = originalDom.copy(
            id = PetId(savedJpa.id!!),
            name = "Bolt",
            breed = "Golden",
            photoUrl = "https://example.com/pets/bolt.png"
        )
        val updatedJpa = petRepoJpa.save(PetMapper.toJpa(updatedDom, existing = savedJpa))

        // Buscar novamente e verificar consistência
        val fetchedJpa = petRepoJpa.findById(updatedJpa.id!!).orElseThrow()
        val fetchedDom = PetMapper.toDomain(fetchedJpa)

        assertEquals(updatedDom.name, fetchedDom.name)
        assertEquals(updatedDom.breed, fetchedDom.breed)
        assertEquals(updatedDom.species, fetchedDom.species)
        assertEquals(updatedDom.photoUrl, fetchedDom.photoUrl)
    }

    @Test
    fun `should delete pet entity and it should no longer exist`() {
        // Persistir tutor e pet
        val tutorJpa = TutorJpa().apply {
            firstName = "Ana"
            email = "ana@ex.com"
            passwordHash = "hash"
            phoneNumber = "1198888-1111"
        }
        tutorRepoJpa.save(tutorJpa)

        val petDom = Pet(
            id = null,
            name = "Rex",
            species = "Cachorro",
            breed = "Labrador",
            birthdate = LocalDate.of(2019, 8, 1),
            photoUrl = "https://example.com/pets/rex.png",
            tutorId = TutorId(tutorJpa.id!!), householdId = householdFor(tutorJpa)
        )
        val savedJpa = petRepoJpa.save(PetMapper.toJpa(petDom))

        // Excluir pet
        petRepoJpa.deleteById(savedJpa.id!!)

        // Garantir que não exista mais
        val result = petRepoJpa.findById(savedJpa.id!!)
        assertTrue(result.isEmpty)
    }
}
