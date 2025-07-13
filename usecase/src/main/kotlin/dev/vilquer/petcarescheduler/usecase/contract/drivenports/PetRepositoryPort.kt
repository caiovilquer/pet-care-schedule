package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.entity.Pet
import dev.vilquer.petcarescheduler.core.domain.entity.PetId

interface PetRepositoryPort {
    fun save(pet: Pet): Pet
    fun findById(id: PetId): Pet?
    fun delete(id: PetId)
    fun findAll(page: Int, size: Int): List<Pet>
    fun countAll(): Long
}