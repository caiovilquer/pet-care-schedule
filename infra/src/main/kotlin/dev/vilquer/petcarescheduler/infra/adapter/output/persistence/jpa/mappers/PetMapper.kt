package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers

import dev.vilquer.petcarescheduler.core.domain.entity.*
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.EventJpa
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
    fun toDomain(jpa: PetJpa): Pet{
        val petId = PetId(jpa.id ?: throw IllegalStateException("Id cannot be null when mapping PetJpa to domain"))

        return Pet(
            id = petId,
            name = jpa.name,
            specie = jpa.specie,
            race = jpa.race,
            birthdate = jpa.birthdate,
            tutorId = jpa.tutorId?.let { TutorId(it) },
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
     * Maps domain entity to JPA entity and can update the original domain entity with generated IDs.
     *
     * @param domain The domain entity to convert
     * @param existing Optional existing JPA entity to update instead of creating a new one
     * @param updateDomain Whether to update the domain entity with IDs generated after persistence
     * @param forceTutorId Optional parameter to force a specific tutor ID when it's not available in the domain
     * @return The corresponding JPA entity
     */
    fun toJpa(
        domain: Pet,
        existing: PetJpa? = null,
        updateDomain: Boolean = false,
        forceTutorId: Long? = null
    ): PetJpa {
        val jpa = existing ?: PetJpa()
        return mapDomainToJpa(domain, jpa, updateDomain, forceTutorId)
    }

    /**
     * Helper function to map domain properties to JPA entity.
     */
    private fun mapDomainToJpa(
        domain: Pet,
        jpa: PetJpa,
        updateDomain: Boolean = false,
        forceTutorId: Long? = null
    ): PetJpa {
        with(domain) {
            jpa.id = id?.value
            jpa.name = name
            jpa.specie = specie
            jpa.race = race
            jpa.birthdate = birthdate

            // Use forceTutorId if provided, otherwise use the tutorId from domain
            // This is crucial when creating pets within a tutor that hasn't been persisted yet
            jpa.tutorId = forceTutorId ?: tutorId?.value
        }

        mapEvents(domain, jpa, updateDomain)
        return jpa
    }

    /**
     * Helper function to map event collections.
     * Can optionally update domain event IDs based on JPA entities.
     */
    private fun mapEvents(domain: Pet, jpa: PetJpa, updateDomain: Boolean = false) {
        // Only proceed with mapping events if pet has an ID or if this is a new pet with no events
        if (jpa.id == null && domain.events.isNotEmpty()) {
            return
        }

        jpa.events.clear()

        if (domain.events.isEmpty()) {
            return
        }

        val existingEventsById = jpa.events.associateBy { it.id }

        // Keep track of domain and JPA event mappings if we need to update IDs
        val eventMappings = mutableListOf<Pair<Event, EventJpa>>()

        val eventsToAdd = domain.events.map { event ->
            val existingEvent = event.id?.value?.let { existingEventsById[it] }
            val eventJpa = EventMapper.toJpa(event, existingEvent)

            if (jpa.id != null) {
                eventJpa.petId = jpa.id!!
            }

            // Store the mapping for later ID updates if needed
            if (updateDomain) {
                eventMappings.add(Pair(event, eventJpa))
            }

            eventJpa
        }

        jpa.events.addAll(eventsToAdd)
    }

    /**
     * Updates a domain entity with IDs from a persisted JPA entity.
     * This should be called after the JPA entity has been persisted.
     *
     * @param domain The domain entity to update
     * @param jpa The persisted JPA entity with generated IDs
     */
    fun updateDomainWithGeneratedIds(domain: Pet, jpa: PetJpa) {
        // Update pet ID using reflection since Pet is immutable
        val idField = Pet::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(domain, jpa.id?.let { PetId(it) })

        // Now that we have a pet ID, we can map the events
        if (domain.events.isNotEmpty() && jpa.events.isEmpty()) {
            val updatedJpa = toJpa(domain, jpa)

            // After adding events, update their IDs in the domain model
            val domainEvents = domain.events.toMutableList()
            val jpaEvents = updatedJpa.events.toList()

            if (domainEvents.size == jpaEvents.size) {
                domainEvents.forEachIndexed { index, event ->
                    val eventJpa = jpaEvents[index]
                    val eventIdField = Event::class.java.getDeclaredField("id")
                    eventIdField.isAccessible = true
                    eventIdField.set(event, eventJpa.id?.let { EventId(it) })
                }
            }
        } else {
            val domainEvents = domain.events.toMutableList()
            val jpaEvents = jpa.events.toList()

            if (domainEvents.size == jpaEvents.size) {
                domainEvents.forEachIndexed { index, event ->
                    val eventJpa = jpaEvents[index]
                    val eventIdField = Event::class.java.getDeclaredField("id")
                    eventIdField.isAccessible = true
                    eventIdField.set(event, eventJpa.id?.let { EventId(it) })
                }
            }
        }
    }
}

// Extension functions for more convenient use within the codebase
fun Pet.toJpa(updateDomain: Boolean = false, forceTutorId: Long? = null): PetJpa =
    PetMapper.toJpa(this, updateDomain = updateDomain, forceTutorId = forceTutorId)
fun PetJpa.toDomain(): Pet = PetMapper.toDomain(this)