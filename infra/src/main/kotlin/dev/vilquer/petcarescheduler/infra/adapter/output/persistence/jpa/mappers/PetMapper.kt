package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers

import dev.vilquer.petcarescheduler.core.domain.entity.*
import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.PetJpa
import java.time.LocalDateTime

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
    fun toDomain(jpa: PetJpa): Pet{
        val petId = PetId(jpa.id ?: throw IllegalStateException("Id cannot be null when mapping PetJpa to domain"))

        return Pet(
            id = petId,
            name = jpa.name,
            specie = jpa.specie,
            race = jpa.race,
            birthdate = jpa.birthdate,
            tutorId = TutorId(jpa.tutorId),
            events = jpa.events.map { eventJpa ->
                Event(
                    id = eventJpa.id?.let { EventId(it) },
                    type = eventJpa.type,
                    description = eventJpa.description,
                    dateStart = eventJpa.dateStart,
                    recurrence = eventJpa.recurrenceEmb?.toDomain(),
                    status = eventJpa.status,
                    petId = petId  // This is the key fix - use the pet's ID
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
    fun toJpa(domain: Pet, existing: PetJpa? = null): PetJpa {
        val jpa = existing ?: PetJpa()
        return mapDomainToJpa(domain, jpa)
    }

    /**
     * Helper function to map domain properties to JPA entity.
     */
    private fun mapDomainToJpa(domain: Pet, jpa: PetJpa): PetJpa {
        with(domain) {
            if (jpa.id == null) jpa.id = id?.value
            jpa.name = name
            jpa.specie = specie
            jpa.race = race
            jpa.birthdate = birthdate
            jpa.tutorId = tutorId.value
        }

        mapEvents(domain, jpa)
        return jpa
    }

    /**
     * Helper function to map event collections.
     */
    private fun mapEvents(domain: Pet, jpa: PetJpa) {
        val existingEventsById = jpa.events.associateBy { it.id }
        jpa.events.clear()

        val eventsToAdd = domain.events.map { event ->
            val existingEvent = event.id?.value?.let { existingEventsById[it] }
            EventMapper.toJpa(event, existingEvent)
        }

        jpa.events.addAll(eventsToAdd)
    }
}
// Extension functions for more convenient use within the codebase
fun Pet.toJpa(): PetJpa = PetMapper.toJpa(this)
fun PetJpa.toDomain(): Pet = PetMapper.toDomain(this)