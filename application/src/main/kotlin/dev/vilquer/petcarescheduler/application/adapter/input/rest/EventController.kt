package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.application.adapter.input.security.CurrentJwt
import dev.vilquer.petcarescheduler.application.adapter.input.security.tutorId
import dev.vilquer.petcarescheduler.application.mapper.EventDtoMapper
import dev.vilquer.petcarescheduler.core.domain.entity.EventId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.command.DeleteEventCommand
import dev.vilquer.petcarescheduler.usecase.command.ToggleEventCommand
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.DeleteEventUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.RegisterEventUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.ToggleEventUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.UpdateEventUseCase
import dev.vilquer.petcarescheduler.usecase.result.EventDetailResult
import dev.vilquer.petcarescheduler.usecase.result.EventRegisteredResult
import jakarta.validation.Valid
import org.springframework.http.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
    fun register(
        @Valid @RequestBody dto: EventDtoMapper.RegisterRequest,
        @AuthenticationPrincipal jwt: CurrentJwt
    ): ResponseEntity<EventRegisteredResult> {
        val tutorId = TutorId(jwt.tutorId())
        val cmd = mapper.toRegisterCommand(dto)
        return ResponseEntity.status(HttpStatus.CREATED).body(registerEvent.execute(cmd, tutorId))
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long, @AuthenticationPrincipal jwt: CurrentJwt) {
        val tutorId = TutorId(jwt.tutorId())
        deleteEvent.execute(DeleteEventCommand(EventId(id)), tutorId)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody dto: EventDtoMapper.UpdateRequest,
        @AuthenticationPrincipal jwt: CurrentJwt
    ): EventDetailResult {
        val tutorId = TutorId(jwt.tutorId())
        return updateEvent.execute(mapper.toUpdateCommand(id, dto), tutorId)
    }

    @PutMapping("/{id}/toggle")
    fun done(@PathVariable id: Long, @AuthenticationPrincipal jwt: CurrentJwt) {
        val tutorId = TutorId(jwt.tutorId())
        toggleEvent.execute(ToggleEventCommand(EventId(id)), tutorId)
    }
}
