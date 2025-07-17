package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.mapper.toDetailResult
import dev.vilquer.petcarescheduler.application.mapper.toSummary
import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PetRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.*
import dev.vilquer.petcarescheduler.usecase.result.*
import org.springframework.stereotype.Service

@Service
class PetAppService(
    private val petRepo: PetRepositoryPort,
    private val tutorRepo: TutorRepositoryPort
):
    CreatePetUseCase,
    ListPetsUseCase,
    UpdatePetUseCase,
    DeletePetUseCase,
    GetPetUseCase {

    override fun execute(cmd: CreatePetCommand): PetCreatedResult {
        val tutor = tutorRepo.findById(cmd.tutorId)
            ?: throw IllegalArgumentException("Tutor ${cmd.tutorId.value} not found")

        val pet = Pet(
            name = cmd.name,
            specie = cmd.specie,
            race = cmd.race,
            birthdate = cmd.birthdate,
            tutorId = tutor.id!!
        )
        val saved = petRepo.save(pet)
        return PetCreatedResult(saved.id!!)
    }

    override fun list(page: Int, size: Int): PetsPageResult {
        val items = petRepo.findAll(page, size).map { it.toSummary() }
        val total = petRepo.countAll()
        return PetsPageResult(items, total, page, size)
    }

    override fun execute(cmd: UpdatePetCommand): PetDetailResult {
        val existing = petRepo.findById(cmd.petId)
            ?: throw IllegalArgumentException("Pet ${cmd.petId.value} not found")
        val updated = existing.copy(
            name = cmd.name ?: existing.name,
            race = cmd.race ?: existing.race,
            birthdate = cmd.birthdate ?: existing.birthdate
        )
        val saved = petRepo.save(updated)
        return saved.toDetailResult()
    }

    override fun execute(cmd: DeletePetCommand) {
        petRepo.delete(cmd.petId)
    }

    override fun get(id: PetId): PetDetailResult =
        petRepo.findById(id)?.toDetailResult()
            ?: throw IllegalArgumentException("Pet ${id.value} not found")
}