package dev.vilquer.petcarescheduler.usecase.command

import dev.vilquer.petcarescheduler.core.domain.entity.EventId

data class ToggleEventCommand(val eventId: EventId)