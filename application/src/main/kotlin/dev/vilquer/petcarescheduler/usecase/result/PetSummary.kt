package dev.vilquer.petcarescheduler.usecase.result

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import java.util.UUID

data class PetSummary(
    val id: PetId,
    val name: String,
    val species: String,
    val photoUrl: String?,
    val photoAssetId: UUID? = null,
)
