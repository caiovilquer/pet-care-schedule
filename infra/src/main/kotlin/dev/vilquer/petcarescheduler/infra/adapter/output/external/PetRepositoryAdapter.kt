package dev.vilquer.petcarescheduler.infra.adapter.output.external

import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PetRepositoryPort
import dev.vilquer.petcarescheduler.core.domain.entity.*
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.toDomain
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.toJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.PetJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class PetRepositoryAdapter(
    private val jpa: PetJpaRepository
) : PetRepositoryPort {

    override fun save(pet: Pet): Pet =
        jpa.save(pet.toJpa()).toDomain()

    override fun findById(id: PetId): Pet? =
        jpa.findById(id.value).orElse(null)?.toDomain()

    override fun delete(id: PetId) =
        jpa.deleteById(id.value)

    override fun findAll(page: Int, size: Int) =
        jpa.findAll(PageRequest.of(page, size)).content.map { it.toDomain() }

    override fun countAll(): Long = jpa.count()

    override fun listByTutor(tutorId: TutorId, page: Int, size: Int): List<Pet> =
        jpa.findAllByTutorId(tutorId.value, PageRequest.of(page, size)).content.map { it.toDomain() }

    override fun countByTutor(tutorId: TutorId): Long =
        jpa.countByTutorId(tutorId.value)

    override fun findByIdAndTutor(id: PetId, tutorId: TutorId): Pet? =
        jpa.findByIdAndTutorId(id.value, tutorId.value)?.toDomain()

    override fun existsForTutor(id: PetId, tutorId: TutorId): Boolean =
        jpa.existsByIdAndTutorId(id.value, tutorId.value)
}
