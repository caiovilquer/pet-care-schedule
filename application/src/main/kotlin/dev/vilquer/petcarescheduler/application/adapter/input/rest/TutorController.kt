package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.application.mapper.TutorDtoMapper
import dev.vilquer.petcarescheduler.application.service.TutorAppService
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.command.DeleteTutorCommand
import dev.vilquer.petcarescheduler.usecase.result.*
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import dev.vilquer.petcarescheduler.usecase.result.TutorDetailResult
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt

@RestController
@RequestMapping("/api/v1/tutors")
class TutorController(
    private val tutorService: TutorAppService,
    private val mapper: TutorDtoMapper
) {

    @PostMapping
    fun create(@RequestBody dto: TutorDtoMapper.CreateRequest): ResponseEntity<TutorCreatedResult> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(tutorService.createTutor(mapper.toCreateCommand(dto)))

    @GetMapping
    fun list(@RequestParam page: Int = 0,
             @RequestParam size: Int = 20): TutorsPageResult =
        tutorService.listTutors(page, size)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody dto: TutorDtoMapper.UpdateRequest
    ): TutorDetailResult =
        mapper.toUpdateCommand(id, dto).let(tutorService::updateTutor)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) =
        tutorService.deleteTutor(DeleteTutorCommand(TutorId(id)))

    @GetMapping("/me")
    fun myProfile(@AuthenticationPrincipal jwt: Jwt): TutorDetailResult {
        val id = TutorId(jwt.subject.toLong())
        return tutorService.getTutor(id)
    }
}
