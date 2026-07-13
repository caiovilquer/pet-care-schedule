package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "care_escalation_outbox")
class CareEscalationOutboxJpa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
    @Column(name = "occurrence_id", nullable = false, unique = true) lateinit var occurrenceId: UUID
    @Column(name = "household_id", nullable = false) lateinit var householdId: UUID
    @Column(name = "recipient_tutor_id", nullable = false) var recipientTutorId: Long = 0
    @Column(name = "recipient_email", nullable = false) lateinit var recipientEmail: String
    @Column(name = "pet_name", nullable = false) lateinit var petName: String
    @Column(name = "care_title", nullable = false) lateinit var careTitle: String
    @Column(name = "due_at", nullable = false) lateinit var dueAt: LocalDateTime
    @Column(name = "created_at", nullable = false) lateinit var createdAt: Instant
    @Column(name = "sent_at") var sentAt: Instant? = null
    @Column(nullable = false) var attempts: Int = 0
}
