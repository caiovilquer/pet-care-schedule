package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId

interface TutorRepositoryPort {
    fun save(tutor: Tutor): Tutor
    fun findById(id: TutorId): Tutor?
    fun findAll(page: Int, size: Int): List<Tutor>
    fun countAll(): Long
    fun delete(id: TutorId)
}