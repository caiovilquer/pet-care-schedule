package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.application.mapper.EventDtoMapper
import dev.vilquer.petcarescheduler.application.service.EventAppService
import dev.vilquer.petcarescheduler.usecase.result.EventRegisteredResult
import org.springframework.http.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/events")
class EventController(
    private val eventService: EventAppService,
    private val mapper: EventDtoMapper
) {
    @PostMapping
    fun register(@RequestBody dto: EventDtoMapper.RegisterRequest): ResponseEntity<EventRegisteredResult> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(eventService.registerEvent(mapper.toRegisterCommand(dto)))
}
