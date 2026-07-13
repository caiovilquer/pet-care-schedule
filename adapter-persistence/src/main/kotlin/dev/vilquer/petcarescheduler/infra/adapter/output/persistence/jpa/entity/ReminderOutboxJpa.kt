package dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "reminder_outbox")
class ReminderOutboxJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "event_id", nullable = false, unique = true)
    var eventId: Long = 0

    @Column(name = "tutor_email", nullable = false)
    lateinit var tutorEmail: String

    @Column(name = "pet_name")
    var petName: String? = null

    @Column(length = 64)
    var timezone: String? = null

    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: Instant

    @Column(name = "sent_at")
    var sentAt: Instant? = null

    @Column(name = "attempts", nullable = false)
    var attempts: Int = 0

    override fun equals(other: Any?): Boolean =
        this === other || (other is ReminderOutboxJpa && this.id != null && this.id == other.id)

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
