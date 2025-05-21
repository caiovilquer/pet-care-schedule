package dev.vilquer.petcarescheduler.dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers

import dev.vilquer.petcarescheduler.core.domain.entity.Event
import dev.vilquer.petcarescheduler.core.domain.entity.EventId

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.EventJpa

object EventMapper {

    fun toDomain(jpa: EventJpa): Event =
        Event(
            id         = jpa.id?.let(::EventId),
            type       = jpa.type,
            description= jpa.description,
            dateStart  = jpa.dateStart,
            recurrence = jpa.recurrenceEmb?.toDomain(),
            status     = jpa.status,
            petId      = PetId(jpa.petId)
        )


    fun toJpa(
        domain: Event,
        existing: EventJpa? = null,
    ): EventJpa {
        val jpa = existing ?: EventJpa()
        jpa.id           = domain.id?.value
        jpa.type         = domain.type
        jpa.description  = domain.description
        jpa.dateStart    = domain.dateStart
        jpa.recurrenceEmb= domain.recurrence?.toEmb()
        jpa.status       = domain.status
        jpa.petId        = domain.petId.value
        return jpa
    }
}
