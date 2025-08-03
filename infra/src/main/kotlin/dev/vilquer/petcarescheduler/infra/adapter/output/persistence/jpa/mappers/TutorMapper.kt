package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers

import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.PetJpa
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
    fun toDomain(jpa: TutorJpa): Tutor {
        val tutorId =
            TutorId(jpa.id ?: throw IllegalStateException("Id cannot be null when mapping TutorJpa to domain"))
        return Tutor(
            id = tutorId,
            firstName = jpa.firstName,
            lastName = jpa.lastName,
            email = Email.of(jpa.email).getOrThrow(),
            passwordHash = jpa.passwordHash,
            phoneNumber = jpa.phoneNumber?.let { PhoneNumber.of(it).getOrThrow()},
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
     * Maps domain entity to JPA entity and can update the original domain entity with generated IDs.
     *
     * @param domain The domain entity to convert
     * @param existing Optional existing JPA entity to update instead of creating a new one
     * @param updateDomain Whether to update the domain entity with IDs generated after persistence
     * @return The corresponding JPA entity
     */
    fun toJpa(
        domain: Tutor,
        existing: TutorJpa? = null,
        updateDomain: Boolean = false
    ): TutorJpa {
        val jpa = existing ?: TutorJpa()
        with(domain) {
            jpa.id = id?.value
            jpa.firstName = firstName
            jpa.lastName = lastName
            jpa.email = email.value
            jpa.passwordHash = passwordHash
            jpa.phoneNumber = phoneNumber?.e164
            jpa.avatar = avatar
        }

        mapPets(domain, jpa, updateDomain)
        return jpa
    }

    /**
     * Helper function to map pet collections.
     * Can optionally update domain pet IDs based on JPA entities.
     */
    private fun mapPets(domain: Tutor, jpa: TutorJpa, updateDomain: Boolean = false) {
        // Only proceed with mapping events if tutor has an ID or if this is a new pet with no events
//        if (jpa.id == null && domain.pets.isNotEmpty()) {
//            return
//        }

        jpa.pets.clear()

        if (domain.pets.isEmpty()) {
            return
        }

        val existingPetsById = if (jpa.id != null) {
            jpa.pets.associateBy { it.id }
        } else {
            emptyMap()
        }

        // Keep track of domain and JPA pet mappings if we need to update IDs
        val petMappings = mutableListOf<Pair<Pet, PetJpa>>()

        val petsToAdd = domain.pets.map { pet ->
            val existingPet = pet.id?.value?.let { existingPetsById[it] }
            val petJpa = PetMapper.toJpa(pet, existingPet, forceTutorId = jpa.id)

            if (jpa.id != null) {
                petJpa.tutorId = jpa.id!!
            }

            // Store the mapping for later ID updates if needed
            if (updateDomain) {
                petMappings.add(Pair(pet, petJpa))
            }

            petJpa
        }

        jpa.pets.addAll(petsToAdd)
    }

    /**
     * Updates a domain entity with IDs from a persisted JPA entity.
     * This should be called after the JPA entity has been persisted.
     *
     * @param domain The domain entity to update
     * @param jpa The persisted JPA entity with generated IDs
     */
    fun updateDomainWithGeneratedIds(domain: Tutor, jpa: TutorJpa) {
        // Update tutor ID using reflection since Tutor is immutable
        val idField = Tutor::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(domain, jpa.id?.let { TutorId(it) })

        val domainPets = domain.pets.toMutableList()
        val jpaPets = jpa.pets.toList()

        if (domainPets.size == jpaPets.size) {
            domainPets.forEachIndexed { index, pet ->
                val petJpa = jpaPets[index]
                val petIdField = Pet::class.java.getDeclaredField("id")
                petIdField.isAccessible = true
                petIdField.set(pet, petJpa.id?.let { PetId(it) })
            }
        }
    }
}

// Extension functions for more convenient use within the codebase
fun Tutor.toJpa(): TutorJpa = TutorMapper.toJpa(this)
fun TutorJpa.toDomain(): Tutor = TutorMapper.toDomain(this)