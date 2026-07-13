package dev.vilquer.petcarescheduler.application.service

import dev.vilquer.petcarescheduler.application.exception.NotFoundException
import dev.vilquer.petcarescheduler.application.mapper.toDetailResult
import dev.vilquer.petcarescheduler.application.mapper.toSummary
import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdAccess
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdPermission
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

    override fun execute(cmd: CreatePetCommand, access: HouseholdAccess): PetCreatedResult {
        requirePermission(access, HouseholdPermission.MANAGE_PETS)
        val tutor = tutorRepo.findById(access.actorTutorId)
            ?: throw NotFoundException("Tutor ${cmd.tutorId.value} not found")

        val pet = Pet(
            name = cmd.name.trim(),
            species = cmd.species.trim(),
            breed = cmd.breed?.trim()?.takeIf { it.isNotEmpty() },
            birthdate = cmd.birthdate,
            photoUrl = cmd.photoUrl,
            tutorId = tutor.id!!,
            householdId = access.householdId,
        )
        val saved = petRepo.save(pet)
        return PetCreatedResult(saved.id!!)
    }

    override fun list(access: HouseholdAccess, page: Int, size: Int): PetsPageResult {
        requirePermission(access, HouseholdPermission.VIEW)
        require(page >= 0) { "page deve ser maior ou igual a zero" }
        require(size in 1..100) { "size deve estar entre 1 e 100" }
        val items = petRepo.listByHousehold(access.householdId, page, size).map { it.toSummary() }
        val total = petRepo.countByHousehold(access.householdId)
        return PetsPageResult(items, total, page, size)
    }

    override fun execute(cmd: UpdatePetCommand, access: HouseholdAccess): PetDetailResult {
        requirePermission(access, HouseholdPermission.MANAGE_PETS)
        val existing = petRepo.findByIdAndHousehold(cmd.petId, access.householdId)
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

    override fun execute(cmd: DeletePetCommand, access: HouseholdAccess) {
        requirePermission(access, HouseholdPermission.MANAGE_PETS)
        if (!petRepo.existsForHousehold(cmd.petId, access.householdId)) throw NotFoundException("Pet não encontrado")
        petRepo.delete(cmd.petId)
    }

    override fun get(id: PetId, access: HouseholdAccess): PetDetailResult {
        requirePermission(access, HouseholdPermission.VIEW)
        val pet = petRepo.findByIdAndHousehold(id, access.householdId)
            ?: throw NotFoundException("Pet ${id.value} not found")
        return pet.toDetailResult(eventRepo.findByPetId(id))
    }

    private fun requirePermission(access: HouseholdAccess, permission: HouseholdPermission) {
        if (!access.can(permission)) throw dev.vilquer.petcarescheduler.application.exception.ForbiddenException("Sem permissão para esta ação")
    }
}
