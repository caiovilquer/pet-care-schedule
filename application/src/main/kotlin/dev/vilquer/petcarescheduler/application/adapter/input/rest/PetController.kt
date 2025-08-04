package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.application.adapter.input.security.CurrentJwt
import dev.vilquer.petcarescheduler.application.adapter.input.security.tutorId
import dev.vilquer.petcarescheduler.application.mapper.PetDtoMapper
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.*
import dev.vilquer.petcarescheduler.usecase.result.*
import jakarta.validation.Valid
import org.springframework.http.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/pets")
class PetController(
    private val mapper: PetDtoMapper,
    private val createPet: CreatePetUseCase,
    private val listPets: ListPetsUseCase,
    private val updatePet: UpdatePetUseCase,
    private val deletePet: DeletePetUseCase,
    private val getPet: GetPetUseCase
) {

    @PostMapping
    fun create(
        @Valid @RequestBody dto: PetDtoMapper.CreateRequest,
        @AuthenticationPrincipal jwt: CurrentJwt
    ): ResponseEntity<PetCreatedResult> {
        val tutorId = TutorId(jwt.tutorId())
        val cmd = mapper.toCreateCommand(dto, tutorId)
        return ResponseEntity.status(HttpStatus.CREATED).body(createPet.execute(cmd))
    }

    @GetMapping
    fun list(
        @AuthenticationPrincipal jwt: CurrentJwt,
        @RequestParam page: Int = 0,
        @RequestParam size: Int = 20
    ): PetsPageResult {
        val tutorId = TutorId(jwt.tutorId())
        return listPets.list(tutorId, page, size)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody dto: PetDtoMapper.UpdateRequest,
        @AuthenticationPrincipal jwt: CurrentJwt
    ): PetDetailResult {
        val tutorId = TutorId(jwt.tutorId())
        return updatePet.execute(mapper.toUpdateCommand(id, dto), tutorId)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long, @AuthenticationPrincipal jwt: CurrentJwt) {
        val tutorId = TutorId(jwt.tutorId())
        deletePet.execute(DeletePetCommand(PetId(id)), tutorId)
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long, @AuthenticationPrincipal jwt: CurrentJwt): PetDetailResult {
        val tutorId = TutorId(jwt.tutorId())
        return getPet.get(PetId(id), tutorId)
    }
}
