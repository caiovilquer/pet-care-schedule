package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId
import dev.vilquer.petcarescheduler.core.domain.report.VeterinaryShare
import dev.vilquer.petcarescheduler.core.domain.report.VeterinaryShareId
import java.time.Instant

interface VeterinaryShareRepositoryPort {
    fun save(share: VeterinaryShare): VeterinaryShare
    fun findActiveByHashForUpdate(tokenHash: String): VeterinaryShare?
    fun findByIdAndHouseholdForUpdate(id: VeterinaryShareId, householdId: HouseholdId): VeterinaryShare?
    fun list(householdId: HouseholdId, petId: PetId?, limit: Int): List<VeterinaryShare>
    fun countActive(householdId: HouseholdId, now: Instant): Long
}
