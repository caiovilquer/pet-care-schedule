package dev.vilquer.petcarescheduler.dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers

import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.PetJpa

object PetMapper {

    fun toDomain(jpa: PetJpa): Pet =
        Pet(
            id         = jpa.id?.let(::PetId),
            name       = jpa.name,
            specie     = jpa.specie,
            race       = jpa.race,
            birthdate  = jpa.birthdate,
            tutorId    = TutorId(jpa.tutorId),
            events      = jpa.events.map { EventMapper.toDomain(it) }
        )


    fun toJpa(
        domain: Pet,
        existing: PetJpa? = null,
    ): PetJpa {
        val jpa = existing ?: PetJpa()
        jpa.id        = domain.id?.value
        jpa.name      = domain.name
        jpa.specie    = domain.specie
        jpa.race      = domain.race
        jpa.birthdate = domain.birthdate
        jpa.tutorId   = domain.tutorId.value

        val existingEventsById = jpa.events.associateBy { it.id }

        val domainEventIds = domain.events.mapNotNull { it.id?.value }.toSet()


        val removals = jpa.events.filter { it.id !in domainEventIds }
        jpa.events.removeAll(removals)

        domain.events.forEach { ed ->
            val eventJpa = ed.id?.value?.let { existingEventsById[it] }
                ?.let { existingEvent -> EventMapper.toJpa(ed, existingEvent) }
                ?: EventMapper.toJpa(ed, null)


            if (ed.id?.value == null || ed.id!!.value !in existingEventsById) {
                jpa.events.add(eventJpa)
            }
        }

        return jpa
    }
}
