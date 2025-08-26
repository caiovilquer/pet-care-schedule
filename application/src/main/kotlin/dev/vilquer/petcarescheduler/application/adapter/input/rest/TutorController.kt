package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.application.adapter.input.security.CurrentJwt
import dev.vilquer.petcarescheduler.application.adapter.input.security.tutorId
import dev.vilquer.petcarescheduler.application.exception.ForbiddenException
import dev.vilquer.petcarescheduler.application.mapper.TutorDtoMapper
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.command.DeleteTutorCommand
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.*
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import dev.vilquer.petcarescheduler.usecase.result.TutorDetailResult
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal

@RestController
@RequestMapping("/api/v1/tutors")
class TutorController(
    private val updateTutor: UpdateTutorUseCase,
    private val deleteTutor: DeleteTutorUseCase,
    private val getTutor: GetTutorUseCase,
    private val mapper: TutorDtoMapper
) {

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody dto: TutorDtoMapper.UpdateRequest,
        @AuthenticationPrincipal jwt: CurrentJwt
    ): TutorDetailResult {
        if (id != jwt.tutorId()) throw ForbiddenException("Não pode alterar outro tutor")
        return mapper.toUpdateCommand(id, dto).let(updateTutor::execute)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long, @AuthenticationPrincipal jwt: CurrentJwt) {
        if (id != jwt.tutorId()) throw ForbiddenException("Não pode alterar outro tutor")
        deleteTutor.execute(DeleteTutorCommand(TutorId(id)))
    }

    @GetMapping("/me")
    fun myProfile(@AuthenticationPrincipal jwt: CurrentJwt): TutorDetailResult {
        val id = TutorId(jwt.tutorId())
        return getTutor.get(id)
    }
}
