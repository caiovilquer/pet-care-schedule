package dev.vilquer.petcarescheduler.adapter.output.persistence.jpa.entity


import com.fasterxml.jackson.annotation.JsonBackReference
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.Event
import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.core.domain.entity.Status
import dev.vilquer.petcarescheduler.core.domain.valueobject.Frequency
import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "event")
data class EventJpa(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Enumerated(EnumType.STRING)
    var type: EventType,
    var description: String?,
    var dateStart: LocalDateTime,
    @Enumerated(EnumType.STRING)
    var frequency: Frequency = Frequency.DAILY,
    @Column(name = "interval_count")
    var interval: Long = 1,
    var repetitions: Int? = null,
    var finalDate: LocalDateTime? = null,
    @Enumerated(EnumType.STRING)
    var status: Status = Status.PENDING,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "pet_id", nullable = false) @JsonBackReference
    var pet: PetJpa
){
    fun toDomain(): Event{
        val eventId = id ?: throw IllegalStateException("Event ID cannot be null when mapping to domain object")
        return Event(EventId(eventId), type, description, dateStart, Recurrence(frequency,interval,repetitions,finalDate), status, pet.toDomain())
    }
}
fun Event.toJpa(): EventJpa = EventJpa(id!!.value, type, description, dateStart,recurrence!!.frequency, recurrence!!.interval, recurrence!!.repetitions, recurrence!!.finalDate, status, pet.toJpa())