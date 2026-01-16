package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.entity.Event

interface NotificationPort {
    fun sendEventReminder(event: Event)
    fun sendEventReminder(event: Event, tutorEmail: String, petName: String?)
}
