package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.application.mapper.TutorDtoMapper
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.command.DeleteTutorCommand
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.*
import dev.vilquer.petcarescheduler.usecase.result.*
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import dev.vilquer.petcarescheduler.usecase.result.TutorDetailResult
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt

@RestController
@RequestMapping("/api/v1/tutors")
class TutorController(
    private val listTutors: ListTutorsUseCase,
    private val updateTutor: UpdateTutorUseCase,
    private val deleteTutor: DeleteTutorUseCase,
    private val getTutor: GetTutorUseCase,
    private val mapper: TutorDtoMapper
) {

    @GetMapping
    fun list(@RequestParam page: Int = 0,
             @RequestParam size: Int = 20): TutorsPageResult =
        listTutors.list(page, size)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody dto: TutorDtoMapper.UpdateRequest
    ): TutorDetailResult =
        mapper.toUpdateCommand(id, dto).let(updateTutor::execute)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) =
        deleteTutor.execute(DeleteTutorCommand(TutorId(id)))

    @GetMapping("/me")
    fun myProfile(@AuthenticationPrincipal jwt: Jwt): TutorDetailResult {
        val id = TutorId(jwt.subject.toLong())
        return getTutor.get(id)
    }
}
