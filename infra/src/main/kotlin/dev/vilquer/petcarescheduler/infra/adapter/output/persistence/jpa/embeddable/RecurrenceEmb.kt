package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.embeddable

import dev.vilquer.petcarescheduler.core.domain.valueobject.Frequency
import jakarta.persistence.*
import java.time.LocalDateTime

@Embeddable
data class RecurrenceEmb(
    @Enumerated(EnumType.STRING)
    var frequency: Frequency,
    var intervalCount: Long? = null,
    var repetitions: Int? = null,
    var finalDate: LocalDateTime? = null
)
