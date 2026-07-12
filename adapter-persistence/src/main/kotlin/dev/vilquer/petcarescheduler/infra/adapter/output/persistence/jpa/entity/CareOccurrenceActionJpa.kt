package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity

import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrenceActionType
import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrenceStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "care_occurrence_action")
class CareOccurrenceActionJpa {
    @Id lateinit var id: UUID
    @Column(name = "request_id", nullable = false, unique = true) lateinit var requestId: UUID
    @Column(name = "occurrence_id", nullable = false) lateinit var occurrenceId: UUID
    @Column(name = "actor_tutor_id", nullable = false) var actorTutorId: Long = 0
    @Enumerated(EnumType.STRING) @Column(nullable = false) lateinit var action: CareOccurrenceActionType
    @Enumerated(EnumType.STRING) @Column(name = "previous_status", nullable = false) lateinit var previousStatus: CareOccurrenceStatus
    @Enumerated(EnumType.STRING) @Column(name = "new_status", nullable = false) lateinit var newStatus: CareOccurrenceStatus
    @Column(name = "happened_at", nullable = false) lateinit var happenedAt: Instant
}
