package dev.vilquer.petcarescheduler.dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers

import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import dev.vilquer.petcarescheduler.dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.embeddable.RecurrenceEmb

fun Recurrence.toEmb(): RecurrenceEmb = RecurrenceEmb(
    frequency   = this.frequency,
    intervalCount    = this.intervalCount,
    repetitions = this.repetitions,
    finalDate   = this.finalDate
)