package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.exception.ForbiddenException
import dev.vilquer.petcarescheduler.application.exception.NotFoundException
import dev.vilquer.petcarescheduler.application.mapper.toDetailResult
import dev.vilquer.petcarescheduler.application.mapper.toSummary
import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.EventRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PetRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.*
import dev.vilquer.petcarescheduler.usecase.result.*

class PetAppService(
    private val petRepo: PetRepositoryPort,
    private val tutorRepo: TutorRepositoryPort,
    private val eventRepo: EventRepositoryPort
):
    CreatePetUseCase,
    ListPetsUseCase,
    UpdatePetUseCase,
    DeletePetUseCase,
    GetPetUseCase {

    override fun execute(cmd: CreatePetCommand): PetCreatedResult {
        val tutor = tutorRepo.findById(cmd.tutorId)
            ?: throw NotFoundException("Tutor ${cmd.tutorId.value} not found")

        val pet = Pet(
            name = cmd.name.trim(),
            species = cmd.species.trim(),
            breed = cmd.breed?.trim()?.takeIf { it.isNotEmpty() },
            birthdate = cmd.birthdate,
            photoUrl = cmd.photoUrl,
            tutorId = tutor.id!!
        )
        val saved = petRepo.save(pet)
        return PetCreatedResult(saved.id!!)
    }

    override fun list(tutorId: TutorId, page: Int, size: Int): PetsPageResult {
        require(page >= 0) { "page deve ser maior ou igual a zero" }
        require(size in 1..100) { "size deve estar entre 1 e 100" }
        val items = petRepo.listByTutor(tutorId, page, size).map { it.toSummary() }
        val total = petRepo.countByTutor(tutorId)
        return PetsPageResult(items, total, page, size)
    }

    override fun execute(cmd: UpdatePetCommand, tutorId: TutorId): PetDetailResult {
        if (!petRepo.existsForTutor(cmd.petId, tutorId))
            throw ForbiddenException("Não pode alterar pet de outro tutor")
        val existing = petRepo.findByIdAndTutor(cmd.petId, tutorId)
            ?: throw NotFoundException("Pet ${cmd.petId.value} not found")
        val updated = existing.copy(
            name = cmd.name.trim(),
            breed = cmd.breed?.trim()?.takeIf { it.isNotEmpty() },
            birthdate = cmd.birthdate,
            photoUrl = cmd.photoUrl?.trim()?.takeIf { it.isNotEmpty() }
        )
        val saved = petRepo.save(updated)
        return saved.toDetailResult(eventRepo.findByPetId(saved.id!!))
    }

    override fun execute(cmd: DeletePetCommand, tutorId: TutorId) {
        if (!petRepo.existsForTutor(cmd.petId, tutorId))
            throw ForbiddenException("Não pode deletar pet de outro tutor")
        petRepo.delete(cmd.petId)
    }

    override fun get(id: PetId, tutorId: TutorId): PetDetailResult {
        val pet = petRepo.findByIdAndTutor(id, tutorId)
            ?: throw NotFoundException("Pet ${id.value} not found")
        return pet.toDetailResult(eventRepo.findByPetId(id))
    }
}
