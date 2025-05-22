package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers

import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.embeddable.RecurrenceEmb

/**
 * Mapper responsible for converting between Recurrence value objects and JPA embeddable objects.
 */
object RecurrenceMapper {
    /**
     * Maps domain value object to JPA embeddable.
     *
     * @param domain The domain value object to convert
     * @return The corresponding JPA embeddable object
     */
    fun toEmb(domain: Recurrence): RecurrenceEmb = RecurrenceEmb(
        frequency = domain.frequency,
        intervalCount = domain.intervalCount,
        repetitions = domain.repetitions,
        finalDate = domain.finalDate
    )

    /**
     * Maps JPA embeddable to domain value object.
     *
     * @param emb The JPA embeddable to convert
     * @return The corresponding domain value object
     * @throws IllegalStateException if frequency is null
     */
    fun toDomain(emb: RecurrenceEmb): Recurrence {
        val frequency = emb.frequency ?: throw IllegalStateException("Frequency cannot be null when mapping RecurrenceEmb to domain")
        return Recurrence(
            frequency = frequency,
            intervalCount = emb.intervalCount ?: 1,
            repetitions = emb.repetitions,
            finalDate = emb.finalDate
        )
    }
}

// Extension functions for more convenient use within the codebase
fun Recurrence.toEmb(): RecurrenceEmb = RecurrenceMapper.toEmb(this)
fun RecurrenceEmb.toDomain(): Recurrence = RecurrenceMapper.toDomain(this)