package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.application.mapper.TutorDtoMapper
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.CreateTutorUseCase
import dev.vilquer.petcarescheduler.usecase.result.TutorCreatedResult
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/api/v1/public"])
class PublicController(
    private val createTutor: CreateTutorUseCase,
    private val mapper: TutorDtoMapper
) {

    @PostMapping("/signup")
    fun create(@RequestBody dto: TutorDtoMapper.CreateRequest): ResponseEntity<TutorCreatedResult> =
        try {
            ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createTutor.execute(mapper.toCreateCommand(dto)))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
}