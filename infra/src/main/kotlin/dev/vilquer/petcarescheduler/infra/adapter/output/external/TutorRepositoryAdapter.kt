package dev.vilquer.petcarescheduler.dev.vilquer.petcarescheduler.infra.adapter.output.external

import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.toDomain
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.toJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.TutorJpaRepository
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class TutorRepositoryAdapter(
    private val jpa: TutorJpaRepository
) : TutorRepositoryPort {

    override fun save(tutor: Tutor): Tutor =
        jpa.save(tutor.toJpa()).toDomain()

    override fun findById(id: TutorId): Tutor? =
        jpa.findById(id.value).orElse(null)?.toDomain()

    override fun delete(id: TutorId) =
        jpa.deleteById(id.value)

    override fun findAll(page: Int, size: Int): List<Tutor> =
        jpa.findAll(PageRequest.of(page, size)).content.map { it.toDomain() }

    override fun countAll(): Long = jpa.count()
}
