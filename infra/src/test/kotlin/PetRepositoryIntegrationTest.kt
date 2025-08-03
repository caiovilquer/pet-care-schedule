package petcarescheduler.infra.test

import dev.vilquer.petcarescheduler.PetCareSchedulerApplication
import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.TutorJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.PetMapper
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.PetJpaRepository
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.TutorJpaRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate

@DataJpaTest
@ContextConfiguration(classes = [PetCareSchedulerApplication::class])
class PetRepositoryIntegrationTest {

    @Autowired
    lateinit var tutorRepoJpa: TutorJpaRepository

    @Autowired
    lateinit var petRepoJpa: PetJpaRepository

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
            specie = "Cachorro",
            race = "Labrador",
            birthdate = LocalDate.of(2019, 8, 1),
            tutorId = TutorId(tutorJpa.id!!)
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
        assertEquals("Cachorro", fetchedDom.specie)
        assertEquals("Labrador", fetchedDom.race)
        assertEquals(LocalDate.of(2019, 8, 1), fetchedDom.birthdate)
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
            specie = "Cachorro",
            race = "Labrador",
            birthdate = LocalDate.of(2019, 8, 1),
            tutorId = TutorId(tutorJpa.id!!)
        )
        val originalJpa = PetMapper.toJpa(originalDom)
        val savedJpa = petRepoJpa.save(originalJpa)

        // Atualizar domínio
        val updatedDom = originalDom.copy(
            id = PetId(savedJpa.id!!),
            name = "Bolt",
            race = "Golden"
        )
        val updatedJpa = petRepoJpa.save(PetMapper.toJpa(updatedDom, existing = savedJpa))

        // Buscar novamente e verificar consistência
        val fetchedJpa = petRepoJpa.findById(updatedJpa.id!!).orElseThrow()
        val fetchedDom = PetMapper.toDomain(fetchedJpa)

        assertEquals(updatedDom.name, fetchedDom.name)
        assertEquals(updatedDom.race, fetchedDom.race)
        assertEquals(updatedDom.specie, fetchedDom.specie)
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
            specie = "Cachorro",
            race = "Labrador",
            birthdate = LocalDate.of(2019, 8, 1),
            tutorId = TutorId(tutorJpa.id!!)
        )
        val savedJpa = petRepoJpa.save(PetMapper.toJpa(petDom))

        // Excluir pet
        petRepoJpa.deleteById(savedJpa.id!!)

        // Garantir que não exista mais
        val result = petRepoJpa.findById(savedJpa.id!!)
        assertTrue(result.isEmpty)
    }
}
