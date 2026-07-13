package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId

interface PetRepositoryPort {
    fun save(pet: Pet): Pet
    fun findById(id: PetId): Pet?
    fun delete(id: PetId)

    fun listByTutor(tutorId: TutorId, page: Int, size: Int): List<Pet>
    fun countByTutor(tutorId: TutorId): Long
    fun findByIdAndTutor(id: PetId, tutorId: TutorId): Pet?
    fun existsForTutor(id: PetId, tutorId: TutorId): Boolean
    fun listByHousehold(householdId: HouseholdId, page: Int, size: Int): List<Pet>
    fun countByHousehold(householdId: HouseholdId): Long
    fun findByIdAndHousehold(id: PetId, householdId: HouseholdId): Pet?
    fun existsForHousehold(id: PetId, householdId: HouseholdId): Boolean

    /** Lista completa (sem paginação), usada para montar o read model do detalhe do tutor. */
    fun findAllByTutor(tutorId: TutorId): List<Pet>
}
