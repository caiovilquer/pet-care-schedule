package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.application.mapper.PetDtoMapper
import dev.vilquer.petcarescheduler.application.service.PetAppService
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.result.*
import org.springframework.http.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/pets")
class PetController(
    private val petService: PetAppService,
    private val mapper: PetDtoMapper
) {

    @PostMapping
    fun create(@RequestBody dto: PetDtoMapper.CreateRequest): ResponseEntity<PetCreatedResult> =
        mapper.toCreateCommand(dto)
            .let(petService::createPet)
            .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @GetMapping
    fun list(
        @RequestParam page: Int = 0,
        @RequestParam size: Int = 20
    ): PetsPageResult = petService.listPets(page, size)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody dto: PetDtoMapper.UpdateRequest
    ): PetDetailResult =
        mapper.toUpdateCommand(id, dto).let(petService::updatePet)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) =
        petService.deletePet(DeletePetCommand(PetId(id)))
}
