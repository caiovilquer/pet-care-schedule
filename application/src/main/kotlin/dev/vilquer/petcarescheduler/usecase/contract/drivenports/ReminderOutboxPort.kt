package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import java.time.Instant

data class ReminderOutboxMessage(
    val id: Long? = null,
    val eventId: EventId,
    val tutorEmail: String,
    val petName: String?,
    val createdAt: Instant,
    val attempts: Int = 0,
)

interface ReminderOutboxPort {
    /** Idempotente: não cria uma segunda mensagem se já existe uma para este evento. */
    fun enqueueIfAbsent(message: ReminderOutboxMessage)

    /** Mensagens ainda não entregues e com tentativas abaixo do limite, das mais antigas para as mais novas. */
    fun findPendingDelivery(maxAttempts: Int, limit: Int): List<ReminderOutboxMessage>

    fun markSent(id: Long)
    fun incrementAttempts(id: Long)
}
