package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.application.mapper.PetDtoMapper
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.*
import dev.vilquer.petcarescheduler.usecase.result.*
import jakarta.validation.Valid
import org.springframework.http.*
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
    fun create(@Valid @RequestBody dto: PetDtoMapper.CreateRequest): ResponseEntity<PetCreatedResult> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(createPet.execute(mapper.toCreateCommand(dto)))

    @GetMapping
    fun list(
        @RequestParam page: Int = 0,
        @RequestParam size: Int = 20
    ): PetsPageResult = listPets.list(page, size)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody dto: PetDtoMapper.UpdateRequest
    ): PetDetailResult =
        mapper.toUpdateCommand(id, dto).let(updatePet::execute)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) =
        deletePet.execute(DeletePetCommand(PetId(id)))

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long) =
        getPet.get(PetId(id))
}
