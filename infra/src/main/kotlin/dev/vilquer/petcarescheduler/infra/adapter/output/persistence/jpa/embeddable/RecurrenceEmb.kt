package dev.vilquer.petcarescheduler.dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.embeddable

import dev.vilquer.petcarescheduler.core.domain.valueobject.Frequency
import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import jakarta.persistence.*
import java.time.LocalDateTime

@Embeddable
data class RecurrenceEmb(
    @Enumerated(EnumType.STRING)
    var frequency: Frequency?,
    var intervalCount: Long?,
    var repetitions: Int?,
    var finalDate: LocalDateTime?
) {
    fun toDomain() = Recurrence(
        frequency ?: throw IllegalStateException("…"),
        intervalCount ?: 1,
        repetitions,
        finalDate
    )
    fun fromDomain(dom: Recurrence) = RecurrenceEmb(
        dom.frequency, dom.intervalCount, dom.repetitions, dom.finalDate
    )
}
fun Recurrence.toJpa() = RecurrenceEmb(frequency, intervalCount, repetitions, finalDate)