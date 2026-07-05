package petcarescheduler.infra.test

import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.PetJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.PetMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PetMapperTest {
    @Test
    fun `should correctly map JPA entity to domain entity`() {
        val petJpa = createTestPetJpa()

        val domainPet = PetMapper.toDomain(petJpa)

        assertJpaToDomainMapping(petJpa, domainPet)
    }

    @Test
    fun `should correctly map domain entity to JPA entity`() {
        val domainPet = createTestPetDomain()

        val petJpa = PetMapper.toJpa(domainPet)

        assertDomainToJpaMapping(domainPet, petJpa)
    }

    @Test
    fun `should maintain bidirectional mapping integrity`() {
        val originalJpa = createTestPetJpa()

        val domain = PetMapper.toDomain(originalJpa)
        val mappedBackToJpa = PetMapper.toJpa(domain)

        assertJpaEquals(originalJpa, mappedBackToJpa)
    }

    private fun assertJpaToDomainMapping(jpa: PetJpa, domain: Pet) {
        assertEquals(jpa.id, domain.id?.value, "Pet ID should be correctly mapped")
        assertEquals(jpa.name, domain.name, "Pet name should be correctly mapped")
        assertEquals(jpa.species, domain.species, "Pet species should be correctly mapped")
        assertEquals(jpa.breed, domain.breed, "Pet breed should be correctly mapped")
        assertEquals(jpa.birthdate, domain.birthdate, "Pet birthdate should be correctly mapped")
        assertEquals(jpa.photoUrl, domain.photoUrl, "Pet photo URL should be correctly mapped")
        assertEquals(jpa.tutorId, domain.tutorId?.value, "Pet tutor ID should be correctly mapped")
    }

    private fun assertDomainToJpaMapping(domain: Pet, jpa: PetJpa) {
        assertEquals(domain.id?.value, jpa.id, "Pet ID should be correctly mapped")
        assertEquals(domain.name, jpa.name, "Pet name should be correctly mapped")
        assertEquals(domain.species, jpa.species, "Pet species should be correctly mapped")
        assertEquals(domain.breed, jpa.breed, "Pet breed should be correctly mapped")
        assertEquals(domain.birthdate, jpa.birthdate, "Pet birthdate should be correctly mapped")
        assertEquals(domain.photoUrl, jpa.photoUrl, "Pet photo URL should be correctly mapped")
        assertEquals(domain.tutorId?.value, jpa.tutorId, "Pet tutor ID should be correctly mapped")
    }

    private fun assertJpaEquals(expected: PetJpa, actual: PetJpa) {
        assertEquals(expected.id, actual.id, "Pet ID should match")
        assertEquals(expected.name, actual.name, "Pet name should match")
        assertEquals(expected.species, actual.species, "Pet species should match")
        assertEquals(expected.breed, actual.breed, "Pet breed should match")
        assertEquals(expected.birthdate, actual.birthdate, "Pet birthdate should match")
        assertEquals(expected.photoUrl, actual.photoUrl, "Pet photo URL should match")
        assertEquals(expected.tutorId, actual.tutorId, "Pet tutor ID should match")
    }

    private fun createTestPetJpa(): PetJpa =
        PetJpa().apply {
            id = 42L
            name = "Bidu"
            species = "Cachorro"
            breed = "SRD"
            birthdate = LocalDate.of(2020, 1, 15)
            photoUrl = "https://example.com/pets/bidu.jpg"
            tutorId = 7L
        }

    private fun createTestPetDomain(): Pet =
        Pet(
            id = PetId(42L),
            name = "Bidu",
            species = "Cachorro",
            breed = "SRD",
            birthdate = LocalDate.of(2020, 1, 15),
            photoUrl = "https://example.com/pets/bidu.jpg",
            tutorId = TutorId(7L)
        )
}
