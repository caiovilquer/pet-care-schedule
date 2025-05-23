package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers

import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.TutorJpa

/**
 * Mapper responsible for converting between Tutor domain entities and JPA entities.
 */
object TutorMapper {
    /**
     * Maps JPA entity to domain entity.
     *
     * @param jpa The JPA entity to convert
     * @return The corresponding domain entity
     */
    fun toDomain(jpa: TutorJpa): Tutor{
        val tutorId = TutorId(jpa.id ?: throw IllegalStateException("Id cannot be null when mapping TutorJpa to domain"))
        return Tutor(
            id = tutorId,
            firstName = jpa.firstName,
            lastName = jpa.lastName,
            email = jpa.email,
            passwordHash = jpa.passwordHash,
            phoneNumber = jpa.phoneNumber,
            avatar = jpa.avatar,
            pets = jpa.pets.map { petJpa ->
                Pet(
                    id = petJpa.id?.let { PetId(it) },
                    name = petJpa.name,
                    specie = petJpa.specie,
                    race = petJpa.race,
                    birthdate = petJpa.birthdate,
                    tutorId = tutorId
                )
            }
        )
    }


    /**
     * Maps domain entity to JPA entity.
     *
     * @param domain The domain entity to convert
     * @param existing Optional existing JPA entity to update instead of creating a new one
     * @return The corresponding JPA entity
     */
    fun toJpa(domain: Tutor, existing: TutorJpa? = null): TutorJpa {
        val jpa = existing ?: TutorJpa()
        return mapDomainToJpa(domain, jpa)
    }

    /**
     * Helper function to map domain properties to JPA entity.
     */
    private fun mapDomainToJpa(domain: Tutor, jpa: TutorJpa): TutorJpa {
        with(domain) {
            jpa.id = id?.value
            jpa.firstName = firstName
            jpa.lastName = lastName
            jpa.email = email
            jpa.passwordHash = passwordHash
            jpa.phoneNumber = phoneNumber
            jpa.avatar = avatar
        }

        mapPets(domain, jpa)
        return jpa
    }

    /**
     * Helper function to map pet collections.
     */
    private fun mapPets(domain: Tutor, jpa: TutorJpa) {
        val existingPetsById = jpa.pets.associateBy { it.id }
        jpa.pets.clear()

        val petsToAdd = domain.pets.map { pet ->
            val existingPet = pet.id?.value?.let { existingPetsById[it] }
            PetMapper.toJpa(pet, existingPet)
        }

        jpa.pets.addAll(petsToAdd)
    }
}
// Extension functions for more convenient use within the codebase
fun Tutor.toJpa(): TutorJpa = TutorMapper.toJpa(this)
fun TutorJpa.toDomain(): Tutor = TutorMapper.toDomain(this)