package dev.vilquer.petcarescheduler.infra.adapter.output.external

import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.toDomain
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers.toJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.TutorJpaRepository
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Locale.getDefault

@Repository
class TutorRepositoryAdapter(
    private val jpa: TutorJpaRepository,
) : TutorRepositoryPort {

    override fun save(tutor: Tutor): Tutor =
        jpa.save(tutor.toJpa()).toDomain()

    override fun findById(id: TutorId): Tutor? =
        jpa.findById(id.value).orElse(null)?.toDomain()

    override fun delete(id: TutorId) =
        jpa.deleteById(id.value)

    @Transactional
    override fun updatePassword(id: TutorId, passwordHash: String) {
        val whenUtc = Instant.now()
        val rows = jpa.setPasswordAndBumpChangedAt(id.value, passwordHash, whenUtc)
        if (rows == 0) throw EntityNotFoundException("Tutor id=$id não encontrado")
    }

    @Transactional
    override fun bumpPasswordChangedAt(id: TutorId, whenUtc: Instant) {
        val rows = jpa.bumpChangedAt(id.value, whenUtc)
        if (rows == 0) throw EntityNotFoundException("Tutor id=$id não encontrado")
    }

    override fun findAll(page: Int, size: Int): List<Tutor> =
        jpa.findAll(PageRequest.of(page, size)).content.map { it.toDomain() }

    override fun findByEmail(email: Email): Tutor? =
        jpa.findByEmail(email.value.lowercase(getDefault()))?.toDomain()

    override fun countAll(): Long = jpa.count()
}
