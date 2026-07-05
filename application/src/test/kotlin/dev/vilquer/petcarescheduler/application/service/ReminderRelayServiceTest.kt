package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.FakeNotifier
import dev.vilquer.petcarescheduler.application.FakeReminderOutboxPort
import dev.vilquer.petcarescheduler.application.InMemoryEventRepo
import dev.vilquer.petcarescheduler.core.domain.entity.Event
import dev.vilquer.petcarescheduler.core.domain.entity.EventType
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.Status
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.ReminderOutboxMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime

class ReminderRelayServiceTest {

    private val eventRepo = InMemoryEventRepo()
    private val outbox = FakeReminderOutboxPort()
    private val notifier = FakeNotifier()
    private val service = ReminderRelayService(outbox, eventRepo, notifier, maxAttempts = 3)

    private fun savedEvent() = eventRepo.save(
        Event(
            type = EventType.VACCINE,
            description = "shot",
            dateStart = LocalDateTime.now(),
            recurrence = null,
            status = Status.PENDING,
            petId = PetId(1)
        )
    )

    @Test
    fun `dispatch marks the message sent on successful delivery`() {
        val event = savedEvent()
        outbox.enqueueIfAbsent(
            ReminderOutboxMessage(eventId = event.id!!, tutorEmail = "tutor@example.com", petName = "Rex", createdAt = Instant.now())
        )
        notifier.deliverySucceeds = true

        service.dispatchPendingReminders()

        assertEquals(1, notifier.notified.size)
        assertTrue(outbox.isSent(outbox.allMessages().first().id))
        assertEquals(0, outbox.findPendingDelivery(maxAttempts = 3, limit = 10).size)
    }

    @Test
    fun `dispatch increments attempts and keeps the message pending on failed delivery`() {
        val event = savedEvent()
        outbox.enqueueIfAbsent(
            ReminderOutboxMessage(eventId = event.id!!, tutorEmail = "tutor@example.com", petName = "Rex", createdAt = Instant.now())
        )
        notifier.deliverySucceeds = false

        service.dispatchPendingReminders()

        val message = outbox.allMessages().first()
        assertEquals(1, message.attempts)
        assertTrue(!outbox.isSent(message.id))
        assertEquals(1, outbox.findPendingDelivery(maxAttempts = 3, limit = 10).size)
    }

    @Test
    fun `dispatch stops retrying once maxAttempts is reached`() {
        val event = savedEvent()
        outbox.enqueueIfAbsent(
            ReminderOutboxMessage(eventId = event.id!!, tutorEmail = "tutor@example.com", petName = "Rex", createdAt = Instant.now())
        )
        notifier.deliverySucceeds = false

        service.dispatchPendingReminders() // attempts: 0 -> 1
        service.dispatchPendingReminders() // attempts: 1 -> 2
        service.dispatchPendingReminders() // attempts: 2 -> 3, agora >= maxAttempts

        assertEquals(3, notifier.notified.size, "as três tentativas devem ter chamado o notifier")
        assertEquals(0, outbox.findPendingDelivery(maxAttempts = 3, limit = 10).size, "esgotada, não deve mais aparecer como pendente")
    }

    @Test
    fun `dispatch marks the message sent when its event was deleted meanwhile`() {
        val event = savedEvent()
        outbox.enqueueIfAbsent(
            ReminderOutboxMessage(eventId = event.id!!, tutorEmail = "tutor@example.com", petName = "Rex", createdAt = Instant.now())
        )
        eventRepo.delete(event.id!!)

        service.dispatchPendingReminders()

        assertEquals(0, notifier.notified.size, "não há mais evento para descrever no e-mail")
        assertTrue(outbox.isSent(outbox.allMessages().first().id))
    }
}
