package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdAccess
import dev.vilquer.petcarescheduler.usecase.command.*
import dev.vilquer.petcarescheduler.usecase.result.*

interface VeterinaryReportUseCase {
    fun summary(query: VeterinarySummaryQuery, access: HouseholdAccess): VeterinarySummaryResult
    fun createShare(command: CreateVeterinaryShareCommand, access: HouseholdAccess): VeterinaryShareCreatedResult
    fun listShares(petId: PetId?, access: HouseholdAccess): List<VeterinaryShareResult>
    fun revoke(command: RevokeVeterinaryShareCommand, access: HouseholdAccess)
    fun publicSummary(command: ResolveVeterinaryShareCommand): PublicVeterinarySummaryResult
    fun sharedAttachmentUrl(command: SharedAttachmentUrlCommand): String
}
