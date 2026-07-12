package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity

import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.embeddable.RecurrenceEmb
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "care_plan")
class CarePlanJpa {
    @Id lateinit var id: UUID
    @Version var version: Long? = null
    @Column(name = "schedule_revision", nullable = false) var scheduleRevision: Int = 0
    @Column(name = "tutor_id", nullable = false) var tutorId: Long = 0
    @Column(name = "pet_id", nullable = false) var petId: Long = 0
    @Column(name = "responsible_tutor_id", nullable = false) var responsibleTutorId: Long = 0
    @Enumerated(EnumType.STRING) @Column(nullable = false) lateinit var type: EventType
    @Column(nullable = false, length = 120) lateinit var title: String
    @Column(length = 2000) var instructions: String? = null
    @Column(name = "start_at", nullable = false) lateinit var startAt: LocalDateTime
    @Embedded var recurrence: RecurrenceEmb? = null
    @Column(name = "reminder_minutes_before", nullable = false) var reminderMinutesBefore: Int = 0
    @Column(nullable = false) var active: Boolean = true
    @Column(name = "created_at", nullable = false) lateinit var createdAt: Instant
    @Column(name = "updated_at", nullable = false) lateinit var updatedAt: Instant

    override fun equals(other: Any?) = this === other || (other is CarePlanJpa && ::id.isInitialized && other::id.isInitialized && id == other.id)
    override fun hashCode() = if (::id.isInitialized) id.hashCode() else 0
}
