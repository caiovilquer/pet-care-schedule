package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers

import dev.vilquer.petcarescheduler.core.domain.entity.Event
import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.EventJpa

/**
 * Mapper responsible for converting between Event domain entities and JPA entities.
 */
object EventMapper {
    /**
     * Maps JPA entity to domain entity.
     *
     * @param jpa The JPA entity to convert
     * @return The corresponding domain entity
     */
    fun toDomain(jpa: EventJpa): Event = Event(
        id = jpa.id?.let(::EventId),
        type = jpa.type,
        description = jpa.description,
        dateStart = jpa.dateStart,
        recurrence = jpa.recurrenceEmb?.toDomain(),
        status = jpa.status,
        petId = jpa.petId?.let { PetId(it) }
    )

    /**
     * Maps domain entity to JPA entity.
     *
     * @param domain The domain entity to convert
     * @param existing Optional existing JPA entity to update instead of creating a new one
     * @return The corresponding JPA entity
     */
    fun toJpa(domain: Event, existing: EventJpa? = null): EventJpa {
        val jpa = existing ?: EventJpa()
        return mapDomainToJpa(domain, jpa)
    }

    /**
     * Helper function to map domain properties to JPA entity.
     */
    private fun mapDomainToJpa(domain: Event, jpa: EventJpa): EventJpa {
        with(domain) {
            jpa.id = id?.value
            jpa.type = type
            jpa.description = description
            jpa.dateStart = dateStart
            jpa.recurrenceEmb = recurrence?.toEmb()
            jpa.status = status
            jpa.petId = petId?.value
        }
        return jpa
    }
}
// Extension functions for more convenient use within the codebase
fun Event.toJpa(): EventJpa = EventMapper.toJpa(this)
fun EventJpa.toDomain(): Event = EventMapper.toDomain(this)
