package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.entity.Event

fun interface NotificationPort {
    fun sendEventReminder(event: Event)
}