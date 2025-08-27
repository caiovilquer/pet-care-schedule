package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email

interface TutorRepositoryPort {
    fun save(tutor: Tutor): Tutor
    fun findById(id: TutorId): Tutor?
    fun findAll(page: Int, size: Int): List<Tutor>
    fun findByEmail(email: Email): Tutor?
    fun countAll(): Long
    fun delete(id: TutorId)
    fun updatePassword(id: TutorId, passwordHash: String)
    fun bumpPasswordChangedAt(id: TutorId, whenUtc: java.time.Instant)
}