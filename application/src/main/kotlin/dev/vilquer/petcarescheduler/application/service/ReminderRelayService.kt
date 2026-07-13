package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.usecase.contract.drivenports.EventRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.EventReminderTarget
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.NotificationPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.ReminderOutboxPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.DispatchPendingRemindersUseCase

/**
 * Entrega, com retry, as mensagens que [EventAppService.sendRemindersForToday]
 * enfileirou. Separar detecção de entrega é o que evita que uma API de e-mail
 * lenta ou fora do ar trave a varredura diária inteira, e dá a cada mensagem
 * uma chance de ser reprocessada em vez de se perder silenciosamente.
 */
class ReminderRelayService(
    private val outbox: ReminderOutboxPort,
    private val eventRepo: EventRepositoryPort,
    private val notifier: NotificationPort,
    private val maxAttempts: Int = 5,
    private val batchSize: Int = 100,
) : DispatchPendingRemindersUseCase {

    override fun dispatchPendingReminders() {
        outbox.findPendingDelivery(maxAttempts, batchSize).forEach { message ->
            val event = eventRepo.findById(message.eventId)
            if (event == null) {
                // O evento foi excluído antes do envio: não há mais o que entregar.
                outbox.markSent(message.id!!)
                return@forEach
            }

            val delivered = notifier.sendEventReminder(
                EventReminderTarget(event, message.tutorEmail, message.petName, message.timezone)
            )
            if (delivered) outbox.markSent(message.id!!) else outbox.incrementAttempts(message.id!!)
        }
    }
}
