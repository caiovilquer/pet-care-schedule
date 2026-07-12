package dev.vilquer.petcarescheduler.usecase.contract.drivenports

interface NotificationPort {
    /** @return true se a entrega foi bem-sucedida; o chamador decide se/quando tentar de novo. */
    fun sendEventReminder(target: EventReminderTarget): Boolean
    fun sendCareReminder(target: CareReminderNotificationTarget): Boolean = false
}
