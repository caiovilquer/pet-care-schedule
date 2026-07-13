package dev.vilquer.petcarescheduler.infra.adapter.output.external

import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.ReminderOutboxJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.ReminderOutboxJpaRepository
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.ReminderOutboxMessage
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.ReminderOutboxPort
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class ReminderOutboxJpaAdapter(
    private val jpa: ReminderOutboxJpaRepository
) : ReminderOutboxPort {

    private val log = LoggerFactory.getLogger(ReminderOutboxJpaAdapter::class.java)

    override fun enqueueIfAbsent(message: ReminderOutboxMessage) {
        if (jpa.existsByEventId(message.eventId.value)) return
        try {
            jpa.save(
                ReminderOutboxJpa().apply {
                    eventId = message.eventId.value
                    tutorEmail = message.tutorEmail
                    petName = message.petName
                    timezone = message.timezone
                    createdAt = message.createdAt
                }
            )
        } catch (ex: DataIntegrityViolationException) {
            // Corrida rara entre o exists() e o save(): outra execução já
            // enfileirou a mesma mensagem primeiro. A constraint UNIQUE já
            // garantiu a idempotência; não há o que fazer além de log.
            log.info("Reminder for event {} already enqueued concurrently", message.eventId.value)
        }
    }

    override fun findPendingDelivery(maxAttempts: Int, limit: Int): List<ReminderOutboxMessage> =
        jpa.findPendingDelivery(maxAttempts, PageRequest.of(0, limit)).map {
            ReminderOutboxMessage(
                id = it.id,
                eventId = EventId(it.eventId),
                tutorEmail = it.tutorEmail,
                petName = it.petName,
                createdAt = it.createdAt,
                attempts = it.attempts,
                timezone = dev.vilquer.petcarescheduler.core.domain.household.HouseholdTimezone.parse(it.timezone).id,
            )
        }

    override fun markSent(id: Long) {
        val entity = jpa.findById(id).orElse(null) ?: return
        entity.sentAt = Instant.now()
        jpa.save(entity)
    }

    override fun incrementAttempts(id: Long) {
        val entity = jpa.findById(id).orElse(null) ?: return
        entity.attempts += 1
        jpa.save(entity)
    }

    override fun resetForEvent(eventId: EventId) {
        jpa.deleteByEventId(eventId.value)
    }
}
