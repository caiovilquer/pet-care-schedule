package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId

interface PetRepositoryPort {
    fun save(pet: Pet): Pet
    fun findById(id: PetId): Pet?
    fun delete(id: PetId)
    fun findAll(page: Int, size: Int): List<Pet>
    fun countAll(): Long

    fun listByTutor(tutorId: TutorId, page: Int, size: Int): List<Pet>
    fun countByTutor(tutorId: TutorId): Long
    fun findByIdAndTutor(id: PetId, tutorId: TutorId): Pet?
    fun existsForTutor(id: PetId, tutorId: TutorId): Boolean
}