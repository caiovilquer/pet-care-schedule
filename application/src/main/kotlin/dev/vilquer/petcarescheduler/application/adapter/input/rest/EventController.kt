package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.application.mapper.EventDtoMapper
import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.usecase.command.DeleteEventCommand
import dev.vilquer.petcarescheduler.usecase.command.ToggleEventCommand
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.DeleteEventUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.RegisterEventUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.ToggleEventUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.UpdateEventUseCase
import dev.vilquer.petcarescheduler.usecase.result.EventDetailResult
import dev.vilquer.petcarescheduler.usecase.result.EventRegisteredResult
import org.springframework.http.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/events")
class EventController(
    private val registerEvent: RegisterEventUseCase,
    private val deleteEvent: DeleteEventUseCase,
    private val updateEvent: UpdateEventUseCase,
    private val toggleEvent: ToggleEventUseCase,
    private val mapper: EventDtoMapper
) {
    @PostMapping
    fun register(@RequestBody dto: EventDtoMapper.RegisterRequest): ResponseEntity<EventRegisteredResult> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(registerEvent.execute(mapper.toRegisterCommand(dto)))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) =
        deleteEvent.execute(DeleteEventCommand(EventId(id)))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody dto: EventDtoMapper.UpdateRequest
    ): EventDetailResult =
        mapper.toUpdateCommand(id, dto).let(updateEvent::execute)

    @PutMapping("/{id}/toggle")
    fun done(@PathVariable id: Long) =
        toggleEvent.execute(ToggleEventCommand(EventId(id)))
}
