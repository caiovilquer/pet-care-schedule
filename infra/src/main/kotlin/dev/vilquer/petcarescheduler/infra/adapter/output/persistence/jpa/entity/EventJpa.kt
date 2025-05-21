package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.Status
import dev.vilquer.petcarescheduler.dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.embeddable.RecurrenceEmb

@Entity
@Table(name = "event")
class EventJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var type: EventType

    var description: String? = null

    @Column(name = "date_start", nullable = false)
    lateinit var dateStart: LocalDateTime

    @Embedded
    var recurrenceEmb: RecurrenceEmb? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var status: Status

    @Column(name = "pet_id", nullable = false)
    var petId: Long = 0

    override fun equals(other: Any?): Boolean =
        this === other || (other is EventJpa && this.id != null && this.id == other.id)

    override fun hashCode(): Int =
        id?.hashCode() ?: 0
}
