package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers

import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.PetJpa

/**
 * Mapper responsible for converting between Pet domain entities and JPA entities.
 */
object PetMapper {
    /**
     * Maps JPA entity to domain entity.
     *
     * @param jpa The JPA entity to convert
     * @return The corresponding domain entity
     */
    fun toDomain(jpa: PetJpa): Pet {
        val petId = PetId(jpa.id ?: throw IllegalStateException("Id cannot be null when mapping PetJpa to domain"))

        return Pet(
            id = petId,
            version = jpa.version,
            name = jpa.name,
            species = jpa.species,
            breed = jpa.breed,
            birthdate = jpa.birthdate,
            photoUrl = jpa.photoUrl,
            photoAssetId = jpa.photoAssetId,
            tutorId = jpa.tutorId?.let { TutorId(it) }
        )
    }

    /**
     * Maps domain entity to JPA entity.
     *
     * @param domain The domain entity to convert
     * @param existing Optional existing JPA entity to update instead of creating a new one
     * @return The corresponding JPA entity
     */
    fun toJpa(domain: Pet, existing: PetJpa? = null): PetJpa {
        val jpa = existing ?: PetJpa()
        with(domain) {
            jpa.id = id?.value
            jpa.version = version
            jpa.name = name
            jpa.species = species
            jpa.breed = breed
            jpa.birthdate = birthdate
            jpa.photoUrl = photoUrl
            jpa.photoAssetId = photoAssetId
            jpa.tutorId = tutorId?.value
        }
        return jpa
    }
}

// Extension functions for more convenient use within the codebase
fun Pet.toJpa(): PetJpa = PetMapper.toJpa(this)
fun PetJpa.toDomain(): Pet = PetMapper.toDomain(this)
