package dev.vilquer.petcarescheduler.usecase.command

import dev.vilquer.petcarescheduler.core.domain.entity.EventId

data class DeleteEventCommand(val eventId: EventId)