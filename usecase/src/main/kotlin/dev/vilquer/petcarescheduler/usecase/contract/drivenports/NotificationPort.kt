package dev.vilquer.petcarescheduler.usecase.contract.drivenports

fun interface NotificationPort {
    fun sendEventReminder(target: EventReminderTarget)
}
