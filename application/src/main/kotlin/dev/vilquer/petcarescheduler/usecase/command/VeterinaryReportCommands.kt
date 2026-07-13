package dev.vilquer.petcarescheduler.usecase.command

import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.core.domain.report.VeterinaryShareId
import java.time.LocalDate
import java.util.UUID

data class VeterinarySummaryQuery(val petId: PetId, val from: LocalDate, val to: LocalDate)

data class CreateVeterinaryShareCommand(
    val petId: PetId,
    val label: String,
    val from: LocalDate,
    val to: LocalDate,
    val expiresInHours: Long,
    val includeNotes: Boolean = false,
    val includeCosts: Boolean = false,
    val includeDocuments: Boolean = false,
)

data class ResolveVeterinaryShareCommand(val token: String)
data class SharedAttachmentUrlCommand(val token: String, val mediaId: UUID)
data class RevokeVeterinaryShareCommand(val shareId: VeterinaryShareId, val expectedVersion: Long)
