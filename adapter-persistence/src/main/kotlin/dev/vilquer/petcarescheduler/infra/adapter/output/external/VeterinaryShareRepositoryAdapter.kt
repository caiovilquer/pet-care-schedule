package dev.vilquer.petcarescheduler.infra.adapter.output.external

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId
import dev.vilquer.petcarescheduler.core.domain.report.*
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity.VeterinaryShareJpa
import dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository.VeterinaryShareJpaRepository
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.VeterinaryShareRepositoryPort
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class VeterinaryShareRepositoryAdapter(private val jpa: VeterinaryShareJpaRepository) : VeterinaryShareRepositoryPort {
    override fun save(share: VeterinaryShare) = jpa.saveAndFlush(share.toJpa()).toDomain()
    override fun findActiveByHashForUpdate(tokenHash: String) = jpa.findActiveByHashForUpdate(tokenHash)?.toDomain()
    override fun findByIdAndHouseholdForUpdate(id: VeterinaryShareId, householdId: HouseholdId) =
        jpa.findForUpdate(id.value, householdId.value)?.toDomain()
    override fun list(householdId: HouseholdId, petId: PetId?, limit: Int) =
        jpa.list(householdId.value, petId?.value, PageRequest.of(0, limit)).map { it.toDomain() }
    override fun countActive(householdId: HouseholdId, now: java.time.Instant) = jpa.countActive(householdId.value, now)
}

private fun VeterinaryShare.toJpa() = VeterinaryShareJpa().also {
    it.id = id.value; it.version = version; it.householdId = householdId.value; it.petId = petId.value
    it.createdByTutorId = createdByTutorId.value; it.label = label; it.tokenHash = tokenHash
    it.periodFrom = scope.from; it.periodTo = scope.to; it.includeNotes = scope.includeNotes
    it.includeCosts = scope.includeCosts; it.includeDocuments = scope.includeDocuments
    it.expiresAt = expiresAt; it.revokedAt = revokedAt; it.createdAt = createdAt
    it.lastAccessedAt = lastAccessedAt; it.accessCount = accessCount
}

private fun VeterinaryShareJpa.toDomain() = VeterinaryShare(
    VeterinaryShareId(id), version, HouseholdId(householdId), PetId(petId), TutorId(createdByTutorId),
    label, tokenHash, VeterinaryShareScope(periodFrom, periodTo, includeNotes, includeCosts, includeDocuments),
    expiresAt, revokedAt, createdAt, lastAccessedAt, accessCount,
)
