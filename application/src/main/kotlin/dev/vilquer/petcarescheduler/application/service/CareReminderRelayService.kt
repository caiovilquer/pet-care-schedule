package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.core.domain.care.CareOccurrenceStatus
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.CareOccurrenceRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.CareReminderNotificationTarget
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.CareReminderOutboxPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.NotificationPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.DispatchPendingCareRemindersUseCase

class CareReminderRelayService(
    private val outbox: CareReminderOutboxPort,
    private val occurrences: CareOccurrenceRepositoryPort,
    private val notifier: NotificationPort,
    private val households: dev.vilquer.petcarescheduler.usecase.contract.drivenports.HouseholdRepositoryPort? = null,
    private val maxAttempts: Int = 5,
    private val batchSize: Int = 100,
) : DispatchPendingCareRemindersUseCase {
    override fun dispatchPendingCareReminders() {
        outbox.findPendingDelivery(maxAttempts, batchSize).forEach { message ->
            val occurrence = occurrences.findByIdAndTutor(
                message.occurrenceId,
                message.tutorId,
            )
            if (occurrence == null || occurrence.status != CareOccurrenceStatus.SCHEDULED) {
                outbox.markSent(message.id!!)
                return@forEach
            }
            val delivered = notifier.sendCareReminder(
                CareReminderNotificationTarget(
                    occurrence.id, occurrence.type, occurrence.title, occurrence.dueAt,
                    message.tutorEmail, message.petName,
                    households?.findById(occurrence.householdId)?.timezone?.id
                        ?: dev.vilquer.petcarescheduler.core.domain.household.HouseholdTimezone.DEFAULT_ID,
                ),
            )
            if (delivered) outbox.markSent(message.id!!) else outbox.incrementAttempts(message.id!!)
        }
    }

}
