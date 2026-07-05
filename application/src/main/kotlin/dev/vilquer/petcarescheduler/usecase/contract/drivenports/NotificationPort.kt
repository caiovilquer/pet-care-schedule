package dev.vilquer.petcarescheduler.usecase.contract.drivenports

fun interface NotificationPort {
    /** @return true se a entrega foi bem-sucedida; o chamador decide se/quando tentar de novo. */
    fun sendEventReminder(target: EventReminderTarget): Boolean
}
