package petcarescheduler.infra.test

import dev.vilquer.petcarescheduler.PetCareSchedulerApplication
import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
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
            phoneNumber = "1234"
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
}
